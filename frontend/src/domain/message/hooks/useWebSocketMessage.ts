import { useEffect, useRef } from 'react'

export const useWebSocketMessage = (
  userId: string | null,
  onMessage: (data: string) => void,
) => {
  const wsRef = useRef<WebSocket | null>(null)

  useEffect(() => {
    if (!userId) return

    const wsUrl = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws/message/${userId}`
    const ws = new WebSocket(wsUrl)
    wsRef.current = ws

    ws.onopen = () => {
      console.log('WebSocket connected')
    }

    ws.onmessage = (event) => {
      onMessage(event.data)
    }

    ws.onclose = () => {
      console.log('WebSocket disconnected')
    }

    ws.onerror = (error) => {
      console.error('WebSocket error:', error)
    }

    return () => {
      ws.close()
    }
  }, [userId, onMessage])
}
