package com.kien.book.service

import org.apache.ibatis.session.ExecutorType
import org.apache.ibatis.session.SqlSession
import org.apache.ibatis.session.SqlSessionFactory
import org.springframework.stereotype.Service

@Service
class BatchService(private val sqlSessionFactory: SqlSessionFactory) {

    // TODO: 次回実装
    fun <T, M> batchProcess(
        dataList: List<T>,
        mapperClass: Class<M>,
        operation: (M, T) -> Any?
    ): Int {

        var sqlSession: SqlSession? = null
        try {
            sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)
            val mapper = sqlSession.getMapper(mapperClass)

            var successfulItems = mutableMapOf<Int, T>()
            var failedItems = mutableMapOf<Int, T>()

            dataList.forEachIndexed { index, item ->
                try {
                    operation(mapper, item)
                    successfulItems.put(index, item)
                } catch (e: Exception) {
                    failedItems.put(index, item)
                }
            }

            sqlSession.commit()


            return 1

        } catch (e: Exception) {
            sqlSession?.rollback()
            throw e
        } finally {
            sqlSession?.close()
        }
    }
}
