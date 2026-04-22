import { useEffect, useState } from 'react'
import {
  CreateNoteParams,
  NoteQueryParams,
  NoteWithRelations,
} from '../types/serviceTypes.ts'
import { noteService } from '../service/noteService.ts'
import { Pagination } from '../../../request'
import { message } from 'antd'

/**
 * 获取笔记列表
 */
export function useNotes(noteQueryParams: NoteQueryParams) {
  const [noteList, setNoteList] = useState<NoteWithRelations[]>([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState<Pagination>()

  async function fetchNotes() {
    setLoading(true)
    const { data, pagination } = await noteService.getNoteList(noteQueryParams)
    setNoteList(data)
    setPagination(pagination)
    setLoading(false)
  }

  useEffect(() => {
    fetchNotes().then()
  }, [
    noteQueryParams,
    noteQueryParams.authorId,
    noteQueryParams.questionId,
    noteQueryParams.collectionId,
    noteQueryParams.page,
    noteQueryParams.pageSize,
    noteQueryParams.sort,
    noteQueryParams.order,
    noteQueryParams.recentDays,
  ])

  async function createNoteHandle(
    questionId: number,
    content: string,
  ): Promise<number | undefined> {
    if (!content.trim()) {
      message.info('笔记内容为空')
      return
    }

    const { data } = await noteService.createNoteService({
      content,
      questionId,
    })

    return data.noteId
  }

  async function updateNoteHandle(
    noteId: number,
    updateBody: CreateNoteParams,
  ) {
    await noteService.updateNoteService(noteId, updateBody)

    setNoteList((prevNoteList) => {
      return prevNoteList.map((note) => {
        if (note.noteId === noteId) {
          return {
            ...note,
            content: updateBody.content,
          }
        }
        return note
      })
    })
  }

  async function deleteNoteHandle(noteId: number) {
    await noteService.deleteNoteService(noteId)
    setNoteList((prevNoteList) =>
      prevNoteList.filter((note) => note.noteId !== noteId),
    )
    message.success('笔记已删除')
  }

  function setNoteLikeStatusHandle(noteId: number, isLiked: boolean) {
    if (!noteId) return
    setNoteList((prevNoteList) => {
      return prevNoteList.map((item) => {
        if (item.noteId === noteId && item.userActions) {
          return {
            ...item,
            likeCount: isLiked ? item.likeCount + 1 : item.likeCount - 1,
            userActions: {
              ...item.userActions,
              isLiked,
            },
          }
        }
        return item
      })
    })
  }

  function setNoteCollectStatusHandle(noteId: number, isCollected: boolean) {
    if (!noteId) return
    setNoteList((prevNoteList) => {
      return prevNoteList.map((item) => {
        if (item.noteId === noteId && item.userActions) {
          return {
            ...item,
            collectCount: isCollected
              ? item.collectCount + 1
              : item.collectCount - 1,
            userActions: {
              ...item.userActions,
              isCollected,
            },
          }
        }
        return item
      })
    })
  }

  return {
    loading,
    noteList,
    pagination,
    createNoteHandle,
    updateNoteHandle,
    deleteNoteHandle,
    setNoteLikeStatusHandle,
    setNoteCollectStatusHandle,
    refreshNotes: fetchNotes,
  }
}
