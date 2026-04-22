import React, { useEffect, useState } from 'react'
import { Menu, Badge } from 'antd'
import { NavLink, useLocation } from 'react-router-dom'
import { MenuProps } from 'antd'
import { useSelector } from 'react-redux'
import { RootState } from '../../../../store/store.ts'
import {
  HOME_PAGE,
  MESSAGE_CENTER,
  NOTE_HOT_RANK,
  QUESTION_LIST,
  QUESTION_SET,
} from '../../router/config.ts'
import Logo from '../logo/Logo.tsx'
import { useApp } from '@/base/hooks'
import { LoginModal, UserAvatarMenu } from '../../../../domain/user'
import SearchInput from '../searchInput/SearchInput.tsx'
import { ColumnDivider } from '../../../../base/components'
import DownloadNoteItem from '../../../../domain/note/components/DownloadNoteItem.tsx'
import { BellOutlined } from '@ant-design/icons'
import { messageService } from '../../../../domain/message/service/messageService.ts'
import { useWebSocketMessage } from '../../../../domain/message/hooks/useWebSocketMessage.ts'

type MenuItem = Required<MenuProps>['items'][number]

const items: MenuItem[] = [
  {
    label: <NavLink to={HOME_PAGE}>首页</NavLink>,
    key: 'home',
  },
  {
    label: <NavLink to={QUESTION_SET}>题库</NavLink>,
    key: 'question-set',
  },
  {
    label: <NavLink to={QUESTION_LIST}>题单</NavLink>,
    key: 'question-list',
  },
  {
    label: <NavLink to={NOTE_HOT_RANK}>热度榜</NavLink>,
    key: 'hot-rank',
  },
]

const NavBar: React.FC = () => {
  /**
   * 监听路由变化，设置选中菜单项
   */
  const [selectedMenuItem, setSelectedMenuItem] = useState<string[]>()
  const location = useLocation()

  /**
   * 未读消息数量状态
   */
  const [unreadCount, setUnreadCount] = useState<number>(0)

  /**
   * 获取 app 信息
   */
  const app = useApp()
  const userId = useSelector((state: RootState) => state.user.userId)

  useEffect(() => {
    if (location.pathname === '/') {
      setSelectedMenuItem(['home'])
    } else {
      setSelectedMenuItem([location.pathname.split('/')[1]])
    }
  }, [location.pathname])

  /**
   * WebSocket 实时接收未读数
   */
  useWebSocketMessage(userId || null, (data) => {
    setUnreadCount(Number(data))
  })

  /**
   * 定时获取未读消息数量（作为 WebSocket 断开时的 fallback）
   */
  useEffect(() => {
    // 如果用户未登录，不获取消息数量
    if (!app.isLogin) {
      setUnreadCount(0)
      return
    }

    // 立即获取一次未读消息数量
    const fetchUnreadCount = async () => {
      try {
        const response = await messageService.getUnreadCount()
        setUnreadCount(response.data)
      } catch (error) {
        console.error('获取未读消息数量失败:', error)
      }
    }

    fetchUnreadCount()

    // 设置30秒定时器作为 fallback
    const interval = setInterval(fetchUnreadCount, 30000)

    // 清理定时器
    return () => {
      clearInterval(interval)
    }
  }, [app.isLogin])

  return (
    <nav className="flex justify-between bg-[#ffffff] px-32 dark:bg-[#141414]">
      <div className="flex items-center gap-2">
        <Logo />
        <Menu
          items={items}
          mode="horizontal"
          style={{ lineHeight: 'var(--header-height)' }}
          selectedKeys={selectedMenuItem}
        />
        <ColumnDivider />
        <div>
          <DownloadNoteItem />
        </div>
      </div>
      <div className="flex items-center gap-8">
        <SearchInput />
        <NavLink to={MESSAGE_CENTER}>
          <Badge count={unreadCount} size="small">
            <BellOutlined className="hover:text-gray-600" />
          </Badge>
        </NavLink>
        {app.isLogin ? <UserAvatarMenu /> : <LoginModal />}
      </div>
    </nav>
  )
}

export default NavBar
