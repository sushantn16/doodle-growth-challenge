# Poll dashboard with an A/B-tested Upgrade button

My solution to the Doodle [Growth Fullstack Engineer challenge](https://github.com/DoodleScheduling/hiring-challenges/tree/master/growth-fullstack-engineer):
a dashboard that lists polls, with an "Upgrade" button whose two versions are A/B tested.

## Start the app

You need Java 21 or newer, and Node 20.19+ (or 22.12+).

```sh
# Terminal 1: backend on http://localhost:8080
cd backend && ./gradlew bootRun

# Terminal 2: frontend on http://localhost:5173
cd frontend && npm install && npm run dev
```

Open http://localhost:5173. To see the results, open http://localhost:5173/api/experiment/results. To land
in a version again as a new visitor, use a private window or delete the `visitor_id` cookie.

Tests: `cd backend && ./gradlew test`, and `cd frontend && npm test`.

## How it works

| Endpoint | What it does |
|---|---|
| `GET /api/polls` | All polls, newest first: `[{id, title, createdAt}]` |
| `GET /api/experiment` | The visitor's version: `{"variant": "A"}`. A new visitor first gets an id cookie |
| `POST /api/experiment/events` | `{"type": "VIEW" or "CLICK", "variant": "A" or "B"}` for the visitor in the cookie. Returns 204; 409 if the visitor has no version or a different one |
| `GET /api/experiment/results` | `{"variants": [{"variant": "A", "saw": 120, "clicked": 9, "clickRate": 0.075}, …]}` |

- **The server creates the visitor id.** It's a random UUID in an HttpOnly, SameSite=Lax cookie that lasts a year.
  - The browser never stores or sends the id itself, so page scripts can't read or change it.
  - It avoids `crypto.randomUUID()`, which doesn't exist on plain-HTTP pages, for example when testing on a phone over the local network.
  - The Vite dev server forwards `/api` to the backend, so the browser sees one origin and the cookie works without CORS.
- **The split:**
  - A new visitor gets a coin flip, `computeIfAbsent(visitorId, …)`, held in a map in memory. After that the visitor always gets the same version, until the server restarts.
  - The random generator is a constructor parameter, so tests can use a seeded one.
- **The counting:**
  - The backend keeps a set of visitor ids per version for "saw" and another for "clicked", so a reload or a second click counts once.
  - A click also counts as a view, so "clicked" can never exceed "saw".
  - Each event says which version the browser showed, and the server counts it only if that matches the visitor's assignment. So an event can never land in a version the visitor didn't see, for example when a brand-new visitor opens two tabs at once and the browser keeps only one of the two cookies.
  - With no visitors, the click rate is 0.
- **The frontend:**
  - The version is requested once per page load, in `main.tsx`, before React renders. Inside a component, React's StrictMode would send the request twice in development, and two requests without a cookie would create two visitor ids.
  - The button sends VIEW once it's on screen and CLICK when pressed.
  - If the version can't be loaded, no button is shown and nothing is recorded. Falling back to A would quietly add visitors to A and skew the split.
  - The button's space is reserved while it loads, so the page doesn't jump.
  - The look follows doodle.com: colours, a black 44 px button, Inter if it's installed. No web font is downloaded, which keeps the first load small on a phone.
- **Phone and keyboard:**
  - It's a native `<button>` with a visible focus outline, reachable with Tab and pressed with Enter or Space.
  - The "coming soon" message after a click is announced to screen readers.
  - Poll titles get `dir="auto"`, because some are Arabic.

## Questions

### 1. What does version B change, and why do you think it will get more clicks?

B changes only the text, from "Upgrade" to **"Remove ads"**.

"Upgrade" tells people what they have to do (pay) but not what they get. "Remove ads" names a concrete benefit that a free user notices on every visit: Doodle's free plan shows ads, and "No ads" is one of the paid plan's features.
My hypothesis is that specific copy that names a benefit gets more clicks than a generic label, because people click when they can see what's in it for them.

The risk is that it's narrower. People who would pay for other features, such as calendar integrations or branding, may not see a reason to click.

### 2. How many visitors would you want before you trust the result? How did you get to that number?

About **8,200 visitors per version, so roughly 16,400 in total**, who saw the button. The calculation:
- Assume a 5% click rate for A, and that the smallest lift worth acting on is 20% relative (5% → 6%).
- A standard two-proportion power calculation, with a 5% false-positive rate (two-sided) and 80% power, gives `n = (1.96 + 0.8416)² × (0.05·0.95 + 0.06·0.94) / 0.01² ≈ 8,155` per version.

The number depends heavily on the assumptions: detecting 5% → 5.5% would need about 31,000 per version. In practice I'd use the real baseline click rate.

I'd also:
- fix the sample size before starting
- not stop early because the results look good on some day
- run for at least one full week, so weekday and weekend visitors are both included.

### 3. The split should be close to 50/50. What could make it come out different, and how would you notice?

What could skew it:
- **Something that differs between the versions.** For example, B's button might fail to render on some browser, so those visitors never send a "saw" event. Only an asymmetry like this moves the ratio; problems that hit both versions equally don't.
- **Assignments that reset or get cached:** a server restart re-randomizes every visitor (this app keeps assignments in memory), and a CDN caching the assignment response would give many visitors the same version. The endpoint sends `Cache-Control: no-store`.
- **Plain chance.** It's small at this size: about ±1% at 10,000 visitors.

Bots that don't keep cookies are filtered out by design: their events arrive without a visitor id and get a 409, so they're never counted. Bots that do keep cookies are split 50/50 like anyone else. They don't move the ratio, but they dilute the click rate.

How I'd notice: run a chi-square test of the "saw" counts against 50/50 (a sample ratio mismatch check), and alert below p < 0.001. For example, out of 10,000 visitors, 5,150 vs 4,850 gives p ≈ 0.003, which is worth a look, and 5,200 vs 4,800 gives p ≈ 0.00006, which means something is broken. Either way, I'd find the cause before trusting the click rates.

### 4. A click on "Upgrade" is not a subscription. What would you measure to know whether version B brings more paid subscriptions?

I'd follow each visitor in the test through the funnel: click → plans page → checkout started → paid. The main metric would be **paid subscriptions per visitor who saw the button**, not per click, so that B can't win by attracting curious clicks that never pay.

Next to that I'd watch:
- revenue per visitor and the plan mix (monthly or annual, cheapest or higher tier)
- refunds and cancellations after 30 days
- trial-to-paid conversion, if there's a trial.

"Remove ads" might bring in people who only want the ads gone and pick the cheapest plan, so revenue matters as much as the count.
Subscriptions are much rarer than clicks, so this needs many more visitors, or a longer test, than question 2.

## What I left out

- **Persistence:** everything is in memory, as the brief allows. Assignments and events are lost on restart.
- **Rendering the version on the server.** With Next.js, the server could render the button directly, so it wouldn't appear only after the page loads. I kept a client-rendered Vite app, which suits a single screen, and reserve the button's space instead.
- **Statistics in the results endpoint:** a confidence interval or p-value for the difference, and the sample ratio mismatch check from question 3. Today it returns raw counts and rates.
- **Events lost after a restart.** If the server restarts while a page is open, that page's events get a 409 and are dropped silently; the next page load gets a new version.
- **A count of assigned visitors per version,** to compare with "saw" and catch tracking bugs that hit only one version.
- **Limits on memory:** every request without a cookie creates a new visitor, so a client could grow the in-memory map without limit. Production would need rate limiting, or assignment by hashing the visitor id instead of storing it.
- **Bot filtering and a production setup:**
  - a `Secure` cookie flag over HTTPS
  - serving the built frontend from Spring
  - rate limiting the events endpoint.
- **End-to-end browser tests in the repo.** The flow was checked in a real browser during development: desktop and phone-sized viewports, keyboard only, and 40 new visitors in a row to confirm each one sends a single version request and that the counts match. The tests in the repo are unit, component and API tests.

## Time and tools

- **Time taken:** _TODO: fill in_
- **AI tools:** _TODO: describe honestly._ I used Claude Code (Claude Opus 5.5) to understand the brief, design the API and data model, write the code and tests, and draft this README.
