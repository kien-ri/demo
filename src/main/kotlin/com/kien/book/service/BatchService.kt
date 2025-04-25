package com.kien.book.service

import org.apache.ibatis.session.ExecutorType
import org.apache.ibatis.session.SqlSession
import org.apache.ibatis.session.SqlSessionFactory
import org.springframework.stereotype.Service

@Service
class BatchService(private val sqlSessionFactory: SqlSessionFactory) {

    fun <T, M> batchProcess(
        dataList: List<T>,
        mapperClass: Class<M>,
        insertOperation: (M, T) -> Any?
    ): Int {

        var sqlSession: SqlSession? = null
        var totalInserted = 0
        try {
            sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)
            val mapper = sqlSession.getMapper(mapperClass)

            dataList.forEach { item ->
                try {
                    insertOperation(mapper, item)
                } catch (e: Exception) {
                    throw RuntimeException("Batch insert failed", e)
                }
            }

            sqlSession.commit()
            return totalInserted

        } catch (e: Exception) {
            sqlSession?.rollback()
            throw e
        } finally {
            sqlSession?.close()
        }
    }
}