import React, { Suspense, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { QuestionView, useQuestion } from '../../../../domain/question'
import {
  MarkdownEditor,
  MarkdownRender,
  Panel,
} from '../../../../base/components'
import { Alert, Button, message, Modal, Spin } from 'antd'
import { Upload } from '@icon-park/react'
import { EyeOutlined } from '@ant-design/icons'
import { NoteList, NoteQueryParams, useNotes } from '../../../../domain/note'
import { useApp } from '../../../../base/hooks'

const QuestionPage: React.FC = () => {
  const { questionId } = useParams()
  const { question, userFinishedQuestion } = useQuestion(Number(questionId))

  const [value, setValue] = useState(question?.userNote.content ?? '')
  const setValueHandle = (value: string) => {
    setValue(value)
  }

  useEffect(() => {
    if (question?.userNote) {
      if (question?.userNote.finished) {
        setValueHandle(question?.userNote.content)
      }
    }
  }, [question])

  const [isEditorVisible, setIsEditorVisible] = useState(false)
  const toggleEditorVisible = () => {
    setIsEditorVisible(!isEditorVisible)
  }

  function writeOrEditButtonHandle() {
    toggleEditorVisible()
  }

  const [noteQueryParams, setNoteQueryParams] = useState<NoteQueryParams>({
    page: 1,
    pageSize: 10,
    questionId: Number(questionId),
  })

  const {
    noteList,
    pagination,
    createNoteHandle,
    updateNoteHandle,
    setNoteLikeStatusHandle,
    setNoteCollectStatusHandle,
  } = useNotes(noteQueryParams)

  const [createBtnLoading, setCreateBtnLoading] = useState(false)
  const app = useApp()

  const buildAuditErrorContent = () => {
    const title = question?.title ? `《${question.title}》` : '当前题目'
    return (
      <div className="space-y-3 text-sm leading-6">
        <div className="font-semibold text-[#8a1f1f]">
          {title} 的笔记未通过审核
        </div>
        <div className="text-[#7a2e2e]">
          你的内容中可能包含违规、引流或攻击性文本，系统已拦截本次发布。
        </div>
        <div className="rounded-xl border border-[#f1b3b3] bg-white/70 px-3 py-2 text-[#8a1f1f]">
          你可以修改后重新提交；审核详情也会同步出现在消息中心。
        </div>
      </div>
    )
  }

  const createOrUpdateNoteClickHandle = async () => {
    if (!app.isLogin) {
      message.info('请先登录')
      return
    }

    setCreateBtnLoading(true)

    try {
      if (!question?.userNote.finished) {
        const noteId = await createNoteHandle(Number(questionId), value)
        toggleEditorVisible()
        if (noteId) {
          userFinishedQuestion(noteId, value)
        }
        message.success('笔记已提交')
      } else {
        if (!question?.userNote) return
        await updateNoteHandle(question?.userNote.noteId, {
          content: value,
          questionId: Number(questionId),
        })
        message.success('笔记已修改')
        toggleEditorVisible()
      }
    } catch (e: any) {
      const errorMessage = e?.message || ''
      if (errorMessage.includes('笔记内容包含违规信息')) {
        message.open({
          key: 'note-audit-reject',
          type: 'error',
          duration: 5,
          content: buildAuditErrorContent(),
          className: 'note-audit-message',
          style: {
            width: 520,
            marginTop: 24,
            padding: 0,
          },
        })
        return
      }
      message.error(errorMessage || '提交失败，请稍后重试')
    } finally {
      setCreateBtnLoading(false)
    }
  }

  const [isShowPreview, setIsShowPreview] = useState(false)

  return (
    <>
      <QuestionView
        question={question}
        writeOrEditButtonHandle={writeOrEditButtonHandle}
      />
      {isEditorVisible && (
        <div className="mb-4 flex w-full justify-center">
          <div className="w-[900px]">
            <Alert
              showIcon
              type="info"
              className="mb-4 rounded-2xl border border-sky-200 bg-sky-50/80"
              message="提交后将自动进行内容审核"
              description="如果命中违规内容，系统会拦截发布，并在消息中心通知你对应的题目。"
            />
            <div className="h-[calc(100vh-var(--header-height)-65px)]">
              <Suspense
                fallback={
                  <Spin tip="加载编辑器中" className="mt-12">
                    {''}
                  </Spin>
                }
              >
                <MarkdownEditor
                  value={value}
                  setValue={setValueHandle}
                ></MarkdownEditor>
              </Suspense>
            </div>
            <div className="sticky bottom-0 z-20 flex justify-end gap-2 border-t border-gray-200 bg-white p-4 shadow">
              <Button
                icon={<EyeOutlined />}
                onClick={() => setIsShowPreview(true)}
              >
                预览笔记
              </Button>
              <Button
                type="primary"
                icon={<Upload />}
                loading={createBtnLoading}
                onClick={createOrUpdateNoteClickHandle}
              >
                {question?.userNote.finished ? '修改笔记' : '提交笔记'}
              </Button>
            </div>
          </div>
        </div>
      )}
      <Modal
        open={isShowPreview}
        onCancel={() => setIsShowPreview(false)}
        footer={null}
        width={1000}
      >
        <MarkdownRender markdown={value} />
      </Modal>
      <div className="flex w-full justify-center">
        <div className="w-[700px]">
          <Panel>
            <NoteList
              showQuestion={false}
              noteList={noteList}
              pagination={pagination}
              queryParams={noteQueryParams}
              setQueryParams={setNoteQueryParams}
              setNoteLikeStatusHandle={setNoteLikeStatusHandle}
              setNoteCollectStatusHandle={setNoteCollectStatusHandle}
            />
          </Panel>
        </div>
      </div>
    </>
  )
}

export default QuestionPage
