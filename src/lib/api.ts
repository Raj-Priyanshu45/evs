import { fallbackCategories, fallbackItems, type WasteCategory, type WasteItem } from '../data/wasteItems'

const API_BASE = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')

export async function loadCatalog(): Promise<{ items: WasteItem[]; categories: WasteCategory[]; connected: boolean }> {
  try {
    const [itemsResponse, categoriesResponse] = await Promise.all([
      fetch(`${API_BASE}/waste-items`),
      fetch(`${API_BASE}/categories`),
    ])
    if (!itemsResponse.ok || !categoriesResponse.ok) throw new Error('API request failed')
    const [items, categories] = await Promise.all([itemsResponse.json(), categoriesResponse.json()]) as [WasteItem[], WasteCategory[]]
    return { items, categories, connected: true }
  } catch {
    return { items: fallbackItems, categories: fallbackCategories, connected: false }
  }
}


export type ImagePrediction = {
  label: string
  score: number
}

export type WasteImageAnalysis = {
  id: string
  originalFilename: string
  storedFilename: string
  imageUrl: string
  contentType: string
  fileSizeBytes: number
  imageWidth: number
  imageHeight: number
  analyzed: boolean
  predictedLabel: string | null
  confidence: number | null
  category: string | null
  categorySlug: string | null
  recyclability: string | null
  disposalType: string | null
  instructions: string[]
  avoid: string[]
  topPredictions: ImagePrediction[]
  message: string
  analyzedAt: string
}

export async function analyzeWasteImage(file: File): Promise<WasteImageAnalysis> {
  const body = new FormData()
  body.append('image', file)
  const response = await fetch(`${API_BASE}/analyze-image`, { method: 'POST', body })
  const payload = await response.json().catch(() => null) as WasteImageAnalysis | { message?: string; detail?: string } | null
  if (!response.ok) {
    const message = payload && ('message' in payload ? payload.message : 'detail' in payload ? payload.detail : undefined)
    throw new Error(message || `Image analysis failed (${response.status}).`)
  }
  if (!payload || !('analyzed' in payload)) throw new Error('The server returned an unexpected image-analysis response.')
  return payload
}
