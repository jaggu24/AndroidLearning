package android.bignerdranch.kotlincriminalintent

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import java.util.*

private const val ARG_Date = "date"
private const val ARG_REQUEST_CODE = "requestCode"
private const val RESULT_DATE_KEY = "resultDate"

class DatePickerFragment : DialogFragment(){


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dateListner = DatePickerDialog.OnDateSetListener{
            _:DatePicker,year: Int,month: Int,day: Int ->
            val resultDate: Date = GregorianCalendar(year,month,day).time

            val result = Bundle().apply {
                putSerializable(RESULT_DATE_KEY,resultDate)
            }

            val resultRequestCode = requireArguments().getString(ARG_REQUEST_CODE,"")
            parentFragmentManager.setFragmentResult(resultRequestCode, result)
        }
        val date = arguments?.getSerializable(ARG_Date) as Date
        val calender = Calendar.getInstance()
        calender.time = date
        val initialYear = calender.get(Calendar.YEAR)
        val initialMonth = calender.get(Calendar.MONTH)
        val initialDay = calender.get(Calendar.DAY_OF_MONTH)

        return DatePickerDialog(
            requireContext(),
            dateListner,
            initialYear,
            initialMonth,
            initialDay
        )
    }

    companion object {
        fun newInstance(date: Date, requestCode: String): DatePickerFragment {
            val args = Bundle().apply {
                putSerializable(ARG_Date, date)
                putString(ARG_REQUEST_CODE, requestCode)
            }
            return DatePickerFragment().apply {
                arguments=args
            }
        }
        fun getSelectedDate(result: Bundle) = result.getSerializable(RESULT_DATE_KEY) as Date
    }
}