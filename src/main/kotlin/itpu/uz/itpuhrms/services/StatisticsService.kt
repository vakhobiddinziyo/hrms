package itpu.uz.itpuhrms.services


import itpu.uz.itpuhrms.*
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.math.RoundingMode


interface StatisticsService {
    fun getEmployeeStatistics(id: Long, startDate: Long, endDate: Long): EmployeeTotalStatistics
}

@Service
class StatisticsServiceImpl(
    private val employeeRepository: EmployeeRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val extraService: ExtraService,
) : StatisticsService {
    override fun getEmployeeStatistics(id: Long, startDate: Long, endDate: Long): EmployeeTotalStatistics {
        val start = startDate.toYYYYMMDD()
        val end = endDate.toYYYYMMDD()
        val organizationId = extraService.getOrgFromCurrentUser().id!!
        val employee = employeeRepository.findByIdAndDeletedFalse(id) ?: throw EmployeeNotFoundException()
        if (organizationId != employee.organization.id!!) throw EmployeeNotFoundException()
        val totalWorkMinutesAndDayQuery = """
            with workingDatesData as (select w.day                   as workDay,
                                 sum(w.required_minutes) as            workMinutes
                          from working_date_config w
                          where w.organization_id = $organizationId
                            and w.deleted = false
                          group by w.day)

        select sum(wdc.workMinutes) as totalWorkingMinutes,
        count(td.id)              as totalWorkingDays
        from table_date td
         JOIN workingDatesData wdc on trim(to_char(td.date, 'DAY')) = wdc.workDay
         where td.organization_id = 1
         and td.date between '${start}' and '${end}'
         and td.type = 'WORK_DAY'
          and td.deleted = false;
        """.trimIndent()

        val totalWorkingMinutesAndDaysData =
            jdbcTemplate.query(
                totalWorkMinutesAndDayQuery,
                BeanPropertyRowMapper(TotalWorkMinutesAndDayResponse::class.java)
            )
                .firstOrNull()
        val taskBasedQuery = """
            WITH pte AS (SELECT tpe.task_id
             FROM project_employee pe
                      JOIN task_project_employee tpe
                           ON pe.id = tpe.employees_id
             WHERE pe.employee_id = ${id}),
            completed_state AS (SELECT DISTINCT ON (st.board_id) st.board_id,
                                                          st.id AS current_state_id
                         FROM state st
                         WHERE st.immutable = TRUE
                           AND st.deleted = FALSE
                         ORDER BY st.board_id, st.ordered DESC),
                customTask AS (SELECT t.*
                    FROM task t
                             JOIN pte
                                  ON t.id = pte.task_id
                                      AND t.deleted = FALSE)
            SELECT coalesce(SUM(ct.time_estimate_amount)
                FILTER (WHERE ct.time_estimate_amount IS NOT NULL and ct.state_id != cs.current_state_id), 0)
           AS totalTaskMinutesNeedToDone,
            SUM(ct.time_estimate_amount) FILTER (WHERE ct.state_id = cs.current_state_id)
            AS totalTaskMinutesAlreadyDone,

            COUNT(ct.id) FILTER (WHERE ct.time_estimate_amount IS NULL)
            AS totalWithoutEstimateAmount
            FROM customTask ct
         LEFT JOIN completed_state cs
                   ON ct.board_id = cs.board_id;
        """.trimIndent()
        val totalTaskInfoData =
            jdbcTemplate.query(taskBasedQuery, BeanPropertyRowMapper(TotalTaskInfoResponse::class.java)).firstOrNull()

        val latencyQuery = """
            WITH filteredDates AS (SELECT td.id,
                                          TRIM(TO_CHAR(td.date, 'DAY')) AS day
                                   FROM table_date td
                                   WHERE td.organization_id = $organizationId
                                     AND td.date BETWEEN '${start}' AND '${end}'
                                     AND td.type = 'WORK_DAY'
                                     AND td.deleted = FALSE),
                 workingDates AS (SELECT w.day        AS day,
                                         w.start_hour AS work_start_hour
                                  FROM working_date_config w
                                  WHERE w.organization_id = $organizationId
                                    AND w.deleted = FALSE)
            SELECT COUNT(ut.id)                                                       AS lateCount,
                   SUM(EXTRACT(EPOCH FROM (ut.time::TIME - wD.work_start_hour)) / 60) AS totalLateMinutes
            FROM user_tourniquet ut
                     INNER JOIN filteredDates fd ON ut.table_date_id = fd.id
                     INNER JOIN workingDates wD ON fd.day = wD.day
            WHERE ut.user_id = ${employee.user!!.id!!}
              AND ut.status = 'LATE'
            GROUP BY ut.user_id;
        """.trimIndent()

        val latencyData =
            jdbcTemplate.query(latencyQuery, BeanPropertyRowMapper(WorkLatencyResponse::class.java)).firstOrNull()

        return EmployeeTotalStatistics(
            employee.imageAsset?.hashId,
            employee.user!!.fullName,
            employee.department.name,
            totalWorkingMinutesAndDaysData?.totalWorkingMinutes,
            totalWorkingMinutesAndDaysData?.totalWorkingDays,
            totalTaskInfoData?.totalWithoutEstimateAmount,
            totalTaskInfoData?.totalTaskMinutesAlreadyDone,
            totalTaskInfoData?.totalTaskMinutesNeedToDone,
            latencyData?.lateCount,
            latencyData?.totalLateMinutes?.setScale(2, RoundingMode.HALF_UP),
        )
    }
}