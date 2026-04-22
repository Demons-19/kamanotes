import React from 'react'
import { Panel } from '../../../../base/components'
import { Divider } from 'antd'
import { NoteHotRankList } from '../../../../domain/note'

const NoteHotRankPage: React.FC = () => {
  return (
    <div className="flex justify-center">
      <div className="w-[800px]">
        <Panel>
          <div className="text-lg font-semibold text-neutral-800">
            笔记热度排行榜
          </div>
          <div className="text-xs text-neutral-500">
            根据笔记的点赞、评论、收藏数据综合计算热度
          </div>
          <Divider />
          <NoteHotRankList limit={50} />
        </Panel>
      </div>
    </div>
  )
}

export default NoteHotRankPage
