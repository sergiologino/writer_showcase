import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const template = readFileSync(resolve(process.cwd(), 'nginx.conf.template'), 'utf8')

describe('production Nginx proxy', () => {
  it('does not resolve the API hostname while starting Nginx', () => {
    const config = template.replace('${API_UPSTREAM}', 'http://missing-api:8080')
    const proxyPasses = [...config.matchAll(/proxy_pass\s+([^;]+);/g)].map((match) => match[1])

    expect(config).toMatch(/resolver\s+127\.0\.0\.11\s+valid=30s\s+ipv6=off;/)
    expect(config).toContain('set $api_upstream http://missing-api:8080;')
    expect(proxyPasses).toHaveLength(5)
    expect(proxyPasses).toEqual(Array(5).fill('$api_upstream'))
    expect(config).toMatch(/location = \/health\s*\{[^}]*return 200 "ok\\n";/)
  })
})
