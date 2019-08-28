package com.blogspot.e_kanivets.moneytracker.activity.record

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import com.blogspot.e_kanivets.moneytracker.R
import com.blogspot.e_kanivets.moneytracker.activity.ReportActivity
import com.blogspot.e_kanivets.moneytracker.activity.base.BaseDrawerActivity
import com.blogspot.e_kanivets.moneytracker.adapter.RecordAdapter
import com.blogspot.e_kanivets.moneytracker.controller.CurrencyController
import com.blogspot.e_kanivets.moneytracker.controller.FormatController
import com.blogspot.e_kanivets.moneytracker.controller.PeriodController
import com.blogspot.e_kanivets.moneytracker.controller.PreferenceController
import com.blogspot.e_kanivets.moneytracker.controller.data.AccountController
import com.blogspot.e_kanivets.moneytracker.controller.data.ExchangeRateController
import com.blogspot.e_kanivets.moneytracker.controller.data.RecordController
import com.blogspot.e_kanivets.moneytracker.entity.Period
import com.blogspot.e_kanivets.moneytracker.entity.RecordItem
import com.blogspot.e_kanivets.moneytracker.entity.data.Record
import com.blogspot.e_kanivets.moneytracker.report.ReportMaker
import com.blogspot.e_kanivets.moneytracker.ui.AppRateDialog
import com.blogspot.e_kanivets.moneytracker.util.AnswersProxy
import com.blogspot.e_kanivets.moneytracker.util.RecordItemsBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import javax.inject.Inject

class MainActivity : BaseDrawerActivity() {

    private lateinit var recordList: List<Record>
    private var recordItems: List<RecordItem> = listOf()
    private lateinit var period: Period
    private lateinit var recordAdapter: RecordAdapter

    @Inject
    lateinit var recordController: RecordController
    @Inject
    lateinit var rateController: ExchangeRateController
    @Inject
    lateinit var accountController: AccountController
    @Inject
    lateinit var currencyController: CurrencyController
    @Inject
    lateinit var preferenceController: PreferenceController
    @Inject
    lateinit var periodController: PeriodController
    @Inject
    lateinit var formatController: FormatController

    private lateinit var tvDefaultAccountTitle: TextView
    private lateinit var tvDefaultAccountSum: TextView
    private lateinit var tvCurrency: TextView

    override fun getContentViewId(): Int = R.layout.activity_main

    override fun initData(): Boolean {
        super.initData()
        appComponent.inject(this)

        preferenceController.addLaunchCount()

        return super.initData()
    }

    override fun initViews() {
        super.initViews()

        setTitle(R.string.title_records)

        if (preferenceController.checkRateDialog()) showAppRateDialog()

        tvDefaultAccountTitle = navigationView.getHeaderView(0).findViewById(R.id.tvDefaultAccountTitle)
        tvDefaultAccountSum = navigationView.getHeaderView(0).findViewById(R.id.tvDefaultAccountSum)
        tvCurrency = navigationView.getHeaderView(0).findViewById(R.id.tvCurrency)

        recordAdapter = RecordAdapter(this, listOf(), true) { position ->
            if (position == 0) showReport()
            else editRecord(position)
        }
        recyclerView.adapter = recordAdapter

        spinner.setPeriodSelectedListener { period ->
            this.period = period
            periodController.writeLastUsedPeriod(period)
            update()
        }

        spinner.setPeriod(periodController.readLastUsedPeriod())

        btnAddExpense.setOnClickListener { addExpense() }
        btnAddIncome.setOnClickListener { addIncome() }
    }

    private fun editRecord(position: Int) {
        AnswersProxy.get().logButton("Edit Record")

        val record = recordList[position - 1 - getCountHeadersItems(position - 1)]
        startAddRecordActivity(record, AddRecordActivity.Mode.MODE_EDIT, record.type)
    }

    private fun addExpense() {
        AnswersProxy.get().logButton("Add Expense")
        startAddRecordActivity(null, AddRecordActivity.Mode.MODE_ADD, Record.TYPE_EXPENSE)
    }

    private fun addIncome() {
        AnswersProxy.get().logButton("Add Income")
        startAddRecordActivity(null, AddRecordActivity.Mode.MODE_ADD, Record.TYPE_INCOME)
    }

    private fun showReport() {
        AnswersProxy.get().logButton("Show Report")
        val intent = Intent(this, ReportActivity::class.java)
        intent.putExtra(ReportActivity.KEY_PERIOD, period)
        startActivity(intent)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == AppCompatActivity.RESULT_OK) {
            when (requestCode) {
                REQUEST_ACTION_RECORD -> update()

                REQUEST_BACKUP -> {
                    appComponent.inject(this)
                    update()
                }

                else -> {
                }
            }
        }
    }

    override fun update() {
        recordList = recordController.getRecordsForPeriod(period)
        recordList = recordList.reversed()
        recordItems = RecordItemsBuilder().getRecordItems(recordList)

        val currency = currencyController.readDefaultCurrency()

        val reportMaker = ReportMaker(rateController)
        val report = reportMaker.getRecordReport(currency, period, recordList)

        recordAdapter.setRecords(recordItems, report, currency, reportMaker.currencyNeeded(currency, recordList))

        fillDefaultAccount()
    }

    private fun getCountHeadersItems(position: Int): Int {
        var countHeadersItems = 0
        for (inOfData in 0 until position) {
            if (recordItems[inOfData] is RecordItem.Header) {
                countHeadersItems++
            }
        }
        return countHeadersItems
    }

    private fun showAppRateDialog() {
        AnswersProxy.get().logEvent("Show App Rate Dialog")
        val dialog = AppRateDialog(this)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    private fun startAddRecordActivity(record: Record?, mode: AddRecordActivity.Mode, type: Int) {
        val intent = Intent(this, AddRecordActivity::class.java)
        intent.putExtra(AddRecordActivity.KEY_RECORD, record)
        intent.putExtra(AddRecordActivity.KEY_MODE, mode)
        intent.putExtra(AddRecordActivity.KEY_TYPE, type)
        startActivityForResult(intent, REQUEST_ACTION_RECORD)
    }

    private fun fillDefaultAccount() {
        val defaultAccount = accountController.readDefaultAccount() ?: return

        tvDefaultAccountSum.text = defaultAccount.title
        tvDefaultAccountSum.text = formatController.formatAmount(defaultAccount.fullSum)
        tvCurrency.text = defaultAccount.currency
    }

    companion object {
        private const val REQUEST_ACTION_RECORD = 6
    }

}