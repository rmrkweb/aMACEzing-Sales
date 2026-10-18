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
        AppSettings::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun settingsDao(): SettingsDao

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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            // Seed Settings
            val settingsDao = database.settingsDao()
            if (settingsDao.getSettingsSnapshot() == null) {
                settingsDao.insertOrUpdate(
                    AppSettings(
                        id = 1,
                        businessNameEngraving = "Engraving Business",
                        businessNameSintra = "Sintra Board Printing Business",
                        engravingOrderPrefix = "ENG",
                        sintraOrderPrefix = "SNT",
                        sintraDefaultMarginPercent = 58.3333,
                        sintraUseCostCalculation = false,
                        currencySymbol = "₱"
                    )
                )
            }

            // Seed Products
            val productDao = database.productDao()
            val initialProducts = listOf(
                // Engraving Business Configuration
                Product(
                    name = "Bamboo Coaster",
                    businessType = BusinessType.ENGRAVING,
                    sellingPrice = 70.0,
                    unitCost = 35.0,
                    isActive = true
                ),
                Product(
                    name = "Bamboo Ballpen",
                    businessType = BusinessType.ENGRAVING,
                    sellingPrice = 40.0,
                    unitCost = 20.0,
                    isActive = true
                ),
                // Sintra Board Business Configuration
                Product(
                    name = "Sintra Board A4 Print",
                    businessType = BusinessType.SINTRA_BOARD,
                    sellingPrice = 150.0,
                    unitCost = 62.50, // 41.6667% cost corresponds to 58.3333% margin
                    isActive = true
                ),
                Product(
                    name = "Sintra Board A3 Print",
                    businessType = BusinessType.SINTRA_BOARD,
                    sellingPrice = 300.0,
                    unitCost = 125.00,
                    isActive = true
                )
            )
            for (product in initialProducts) {
                val existing = productDao.getProductByNameAndBusiness(product.name, product.businessType)
                if (existing == null) {
                    productDao.insertProduct(product)
                }
            }
        }
    }
}
