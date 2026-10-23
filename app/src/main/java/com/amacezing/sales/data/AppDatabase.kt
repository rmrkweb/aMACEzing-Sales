package com.amacezing.sales.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.amacezing.sales.model.AppSettings
import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.model.Customer
import com.amacezing.sales.model.PaymentRecord
import com.amacezing.sales.model.Product
import com.amacezing.sales.model.SalesTransaction
import com.amacezing.sales.model.TransactionLineItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Product::class,
        Customer::class,
        SalesTransaction::class,
        TransactionLineItem::class,
        PaymentRecord::class,
        AppSettings::class,
        com.amacezing.sales.model.BusinessProfile::class,
        com.amacezing.sales.model.Quotation::class,
        com.amacezing.sales.model.QuotationLineItem::class,
        com.amacezing.sales.model.Business::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun settingsDao(): SettingsDao
    abstract fun businessProfileDao(): BusinessProfileDao
    abstract fun quotationDao(): QuotationDao
    abstract fun businessDao(): BusinessDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "amacezing_sales.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default seed data on background thread
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    populateInitialData(database)
                                }
                            }
                        }
                    })
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }


        /**
         * v3 -> v4: adds the `businesses` table. Existing orders, products and quotations keep
         * their `businessType` text values ("ENGRAVING" / "SINTRA_BOARD"), which become business ids,
         * so no existing row is modified. The two original businesses are seeded from the
         * current settings so names, prefixes and the Sintra margin rules carry over.
         */
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `businesses` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`orderPrefix` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `profitMethod` TEXT NOT NULL, " +
                        "`marginPercent` REAL NOT NULL, `useUnitCostIfAvailable` INTEGER NOT NULL, " +
                        "`isActive` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO businesses (id, name, orderPrefix, colorHex, profitMethod, " +
                        "marginPercent, useUnitCostIfAvailable, isActive, sortOrder) " +
                        "SELECT 'ENGRAVING', 'Engraving', engravingOrderPrefix, '#00B8F0', 'UNIT_COST', " +
                        "0.0, 0, 1, 0 FROM app_settings WHERE id = 1"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO businesses (id, name, orderPrefix, colorHex, profitMethod, " +
                        "marginPercent, useUnitCostIfAvailable, isActive, sortOrder) " +
                        "SELECT 'SINTRA_BOARD', 'Sintra Board', sintraOrderPrefix, '#A78BFA', 'FIXED_MARGIN', " +
                        "sintraDefaultMarginPercent, sintraUseCostCalculation, 1, 1 FROM app_settings WHERE id = 1"
                )
                // Fresh/odd databases without a settings row: fall back to defaults.
                db.execSQL(
                    "INSERT OR IGNORE INTO businesses VALUES ('ENGRAVING', 'Engraving', 'ENG', '#00B8F0', " +
                        "'UNIT_COST', 0.0, 0, 1, 0)"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO businesses VALUES ('SINTRA_BOARD', 'Sintra Board', 'SNT', '#A78BFA', " +
                        "'FIXED_MARGIN', 58.3333, 0, 1, 1)"
                )
            }
        }

        /** v4 -> v5: businesses get an optional icon key (nullable TEXT, no default). */
        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `businesses` ADD COLUMN `iconKey` TEXT")
            }
        }

        /** Only the settings row is created on first run; no businesses, products or profile are seeded. */
        suspend fun populateInitialData(database: AppDatabase) {
            // Seed Settings
            val settingsDao = database.settingsDao()
            if (settingsDao.getSettingsSnapshot() == null) {
                settingsDao.insertOrUpdate(
                    AppSettings(id = 1)
                )
            }
        }
    }
}
