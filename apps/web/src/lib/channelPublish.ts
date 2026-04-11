import type { ChannelType, PostOutboundInfo } from '../api/types'

export function channelShortName(t: ChannelType): string {
  const m: Record<ChannelType, string> = {
    TELEGRAM: 'TG',
    VK: 'ВК',
    ODNOKLASSNIKI: 'ОК',
  }
  return m[t] ?? t
}

export function channelFullName(t: ChannelType): string {
  const m: Record<ChannelType, string> = {
    TELEGRAM: 'Telegram',
    VK: 'ВКонтакте',
    ODNOKLASSNIKI: 'Одноклассники',
  }
  return m[t] ?? t
}

export function deliveryStatusLabel(status: PostOutboundInfo['deliveryStatus']): string {
  switch (status) {
    case 'PENDING':
      return 'Ожидает публикации'
    case 'SENT':
      return 'Опубликовано'
    case 'FAILED':
      return 'Не опубликовано'
    case 'REJECTED':
      return 'Отклонено модератором'
    default:
      return String(status)
  }
}

export function deliveryStatusTone(
  status: PostOutboundInfo['deliveryStatus']
): 'ok' | 'wait' | 'bad' | 'warn' {
  switch (status) {
    case 'SENT':
      return 'ok'
    case 'PENDING':
      return 'wait'
    case 'REJECTED':
      return 'warn'
    case 'FAILED':
      return 'bad'
    default:
      return 'wait'
  }
}
