import { ApiList } from '../../../request'

export const noteApiList: ApiList = {
  getNoteList: ['GET', '/api/notes'],
  getNoteDetail: ['GET', '/api/notes/{noteId}'],
  createNote: ['POST', '/api/notes'],
  updateNote: ['PATCH', '/api/notes/{noteId}'],
  deleteNote: ['DELETE', '/api/notes/{noteId}'],
  getNoteRankList: ['GET', '/api/notes/ranklist'],
  getHeatMap: ['GET', '/api/notes/heatmap'],
  getTop3Count: ['GET', '/api/notes/top3count'],
  getHotRank: ['GET', '/api/notes/hot-rank'],
  downloadNote: ['GET', '/api/notes/download'],
}
