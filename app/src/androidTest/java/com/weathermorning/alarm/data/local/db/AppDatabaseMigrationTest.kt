package com.weathermorning.alarm.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2AddsTypedReasonColumns() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO alarm_history (
                    id,
                    occurredAtEpochMillis,
                    type,
                    reason,
                    weatherCondition,
                    precipitationProbability,
                    source
                ) VALUES (
                    'legacy-1',
                    1,
                    'PRECHECK',
                    'Alarm disabled',
                    NULL,
                    NULL,
                    NULL
                )
                """.trimIndent(),
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            AppDatabase.MIGRATION_1_2,
        )

        db.query(
            """
            SELECT
                reasonCode,
                reasonCondition,
                reasonValue1,
                reasonValue2
            FROM alarm_history
            WHERE id = 'legacy-1'
            """.trimIndent(),
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(true, cursor.isNull(0))
            assertEquals(true, cursor.isNull(1))
            assertEquals(true, cursor.isNull(2))
            assertEquals(true, cursor.isNull(3))
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
