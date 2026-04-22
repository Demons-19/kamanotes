import { useDispatch } from 'react-redux'
import { resetUser } from '../../../store/userSlice.ts'
import { logout } from '../../../store/appSlice.ts'
import { kamanoteUserToken } from '../../../base/constants'
import { userService } from '../service/userService.ts'

export function useLogout() {
  const dispatch = useDispatch()
  return async () => {
    try {
      await userService.logoutService()
    } catch {
      // 即使后端请求失败也继续清理本地状态
    }
    localStorage.removeItem(kamanoteUserToken)
    dispatch(resetUser())
    dispatch(logout())
  }
}
