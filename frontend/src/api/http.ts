/** GETs `url` and parses the JSON body. Throws if the response isn't a 2xx. */
export async function getJson<T>(url: string): Promise<T> {
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`GET ${url} failed with ${response.status}`)
  }
  return response.json() as Promise<T>
}
