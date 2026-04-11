import { apiFetch } from './client'

export interface CategoryDto {
  id: number
  name: string
  slug: string
  description: string | null
  color: string | null
  createdAt: string
  updatedAt: string
}

export interface CategoryCreatePayload {
  name: string
  slug?: string
  description?: string
  color?: string
}

export function fetchCategories(): Promise<CategoryDto[]> {
  return apiFetch<CategoryDto[]>('/api/categories')
}

export function createCategory(payload: CategoryCreatePayload): Promise<CategoryDto> {
  return apiFetch<CategoryDto>('/api/categories', {
    method: 'POST',
    body: JSON.stringify({
      name: payload.name.trim(),
      slug: payload.slug?.trim() || undefined,
      description: payload.description ?? undefined,
      color: payload.color ?? undefined,
    }),
  })
}
