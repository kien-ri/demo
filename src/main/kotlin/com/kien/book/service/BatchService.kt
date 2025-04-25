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
        operation: (M, T) -> Any?
    ): Int {

        var sqlSession: SqlSession? = null
        var totalCount = 0
        try {
            sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)
            val mapper = sqlSession.getMapper(mapperClass)

            dataList.forEach { item ->
                try {
                    operation(mapper, item)
                    totalCount++
                } catch (e: Exception) {
                    throw RuntimeException("Batch opration failed", e)
                }
            }

            sqlSession.commit()
            return totalCount

        } catch (e: Exception) {
            sqlSession?.rollback()
            throw e
        } finally {
            sqlSession?.close()
        }
    }
}