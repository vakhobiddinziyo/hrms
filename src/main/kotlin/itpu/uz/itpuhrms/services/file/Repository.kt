package itpu.uz.itpuhrms.services.file

import itpu.uz.itpuhrms.FileAsset
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.stereotype.Repository


@Repository
interface FileAssetRepository : BaseRepository<FileAsset> {
    fun findByHashIdAndDeletedFalse(hashId: String): FileAsset?
    fun existsByHashId(hashId: String): Boolean
    fun findAllByActiveFalseAndDeletedFalse(): List<FileAsset>
    fun findAllByHashIdInAndDeletedFalse(hashId: List<String>): MutableList<FileAsset>
}