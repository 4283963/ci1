import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import { useDashboardStore } from '@/store/dashboard'

let stompClient = null

export function connectWebSocket() {
  const store = useDashboardStore()

  if (stompClient && stompClient.connected) {
    return
  }

  try {
    const socket = new SockJS('/api/ws')
    stompClient = Stomp.over(socket)
    stompClient.debug = () => {}

    stompClient.connect(
      {},
      () => {
        console.log('WebSocket连接成功')
        store.setWsConnected(true)
        stompClient.subscribe('/topic/realtime-rooms', (message) => {
          try {
            const data = JSON.parse(message.body)
            store.setRealtimeData(data)
          } catch (e) {
            console.error('解析WS消息失败', e)
          }
        })
      },
      (error) => {
        console.error('WebSocket连接失败', error)
        store.setWsConnected(false)
        setTimeout(connectWebSocket, 5000)
      }
    )
  } catch (e) {
    console.error('创建WS连接异常', e)
    setTimeout(connectWebSocket, 5000)
  }
}

export function disconnectWebSocket() {
  if (stompClient) {
    stompClient.disconnect(() => {
      console.log('WebSocket已断开')
    })
    stompClient = null
    useDashboardStore().setWsConnected(false)
  }
}
