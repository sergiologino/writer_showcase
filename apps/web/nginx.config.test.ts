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
    expect(proxyPasses).toHaveLength(7)
    expect(proxyPasses).toEqual(Array(7).fill('$api_upstream'))
    expect(config).toMatch(/location = \/health\s*\{[^}]*return 200 "ok\\n";/)
    expect(config).toMatch(/location = \/\s*\{[^}]*proxy_pass \$api_upstream;/)
    expect(config).toMatch(/location \/blog\/\s*\{[^}]*proxy_pass \$api_upstream;/)
    expect(config).toMatch(/location = \/assets\/app\.js\s*\{[^}]*Cache-Control "no-cache";/)
    expect(config).toMatch(/location \/\s*\{[^}]*X-Robots-Tag "noindex, nofollow" always;/)
  })
})
