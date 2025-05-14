package itpu.uz.itpuhrms.services.user

import itpu.uz.itpuhrms.User
import itpu.uz.itpuhrms.UserCredentials
import itpu.uz.itpuhrms.base.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository


@Repository
interface UserRepository : BaseRepository<User> {
    fun findByUsernameAndDeletedFalse(username: String): User?

    @Query(
        """
        select u.*
        from users u
        where ((:role is null or u.role = :role))
          and not u.role = 'DEVELOPER'
          and u.deleted = false
          and (:search is null or u.full_name ilike concat('%', :search, '%'))
        order by u.username
    """, countQuery = """
        select count(u.id)
        from users u
        where ((:role is null or u.role = :role))
          and not u.role = 'DEVELOPER'
          and u.deleted = false
          and (:search is null or u.full_name ilike concat('%', :search, '%'))
    """, nativeQuery = true
    )
    fun findAllUser(@Param("role") role: String?, search: String?, pageable: Pageable): Page<User>

    @Query(
        """
        select u.*
        from users u
                 join employee e on u.id = e.user_id
        where e.organization_id = :orgId
          and e.deleted = false
          and u.deleted = false
    """, nativeQuery = true
    )
    fun findOrganizationUsers(orgId: Long): MutableList<User>
    fun existsByUsername(username: String): Boolean

}


@Repository
interface UserCredentialsRepository : BaseRepository<UserCredentials> {
    fun findByPinfl(pinfl: String): UserCredentials?
    fun findByUserIdAndDeletedFalse(userId: Long): UserCredentials?
    fun findByPinflAndDeletedFalse(pinfl: String): UserCredentials?
    fun existsByPinfl(pinfl: String): Boolean
    fun existsByUserIdAndDeletedFalse(userId: Long): Boolean

    @Query(
        """
            select uc.*
            from user_credentials uc
                     join users u on uc.user_id = u.id
            where u.role = 'USER'
              and (:search is null or uc.fio ilike concat(:search, '%')
                or uc.card_serial_number ilike concat(:search, '%')
                or uc.pinfl ilike concat(:search, '%'))
              and (:gender is null or uc.gender = :gender)
              and u.deleted = false
    """, nativeQuery = true,
        countQuery = """
            select count(uc.id)
            from user_credentials uc
                     join users u on uc.user_id = u.id
            where u.role = 'USER'
              and (:search is null or uc.fio ilike concat(:search, '%')
                or uc.card_serial_number ilike concat(:search, '%')
                or uc.pinfl ilike concat(:search, '%'))
              and (:gender is null or uc.gender = :gender)
              and u.deleted = false
        """
    )
    fun findClientsWithFilter(
        @Param("search") search: String?,
        @Param("gender") gender: String?,
        pageable: Pageable
    ): Page<UserCredentials>


    fun existsByCardSerialNumber(cardSerialNumber: String): Boolean
}
