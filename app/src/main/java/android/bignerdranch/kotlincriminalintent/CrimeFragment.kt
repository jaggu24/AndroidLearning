package android.bignerdranch.kotlincriminalintent

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.lifecycle.ViewModelProviders
import java.util.*
import androidx.lifecycle.Observer
import java.io.File

import java.text.SimpleDateFormat

private const val ARG_CRIME_ID = "crime_id"
private const val TAG = "CrimeFragment"
private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = "DialogDate"
private const val REQUEST_CONTACT = 1
private const val REQUEST_PHOTO = 2
private const val DATE_FORMAT = "EEE, MMM, dd"

class CrimeFragment: Fragment(), FragmentResultListener {
    private lateinit var crime: Crime
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView
    private lateinit var pickContactContract: ActivityResultContract<Uri,Uri?>
    private lateinit var pickContactCallback: ActivityResultCallback<Uri?>
    private lateinit var pickContactLauncher: ActivityResultLauncher<Uri>
    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProviders.of(this).get(CrimeDetailViewModel::class.java)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId:UUID=arguments?.getSerializable(ARG_CRIME_ID) as UUID
        Log.d(TAG, "args bundle cime ID: $crimeId")
        crimeDetailViewModel.loadCrime(crimeId)
        pickContactContract=object : ActivityResultContract<Uri,Uri?>(){
            override fun createIntent(context: Context, input: Uri?): Intent {
                Log.d(TAG,"createIntent() called")
                return Intent(Intent.ACTION_PICK, input)
            }

            override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
                Log.d(TAG,"ParseResult() called")
                if(resultCode!=Activity.RESULT_OK || intent == null){
                    return null
                }
                return intent.data
            }
        }
        pickContactCallback = ActivityResultCallback<Uri?> { contactUri: Uri? ->
            Log.d(TAG,"OnActivityResult() called with result: ")
            val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
            val cursor = contactUri?.let { requireActivity().contentResolver.query(it,queryFields,null,null,null) }
            cursor?.use {

                it.moveToFirst()
                val suspect = it.getString(0)
                crime.suspect = suspect
                crimeDetailViewModel.saveCrime(crime)
                suspectButton.text = suspect
            }
        }
        pickContactLauncher = registerForActivityResult(pickContactContract,pickContactCallback)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime,container,false)
        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox
        reportButton=view.findViewById(R.id.crime_report) as Button
        suspectButton = view.findViewById(R.id.crime_suspect) as Button
        photoButton = view.findViewById(R.id.crime_camera) as ImageButton
        photoView = view.findViewById(R.id.crime_photo) as ImageView
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
                Observer { crime ->
                    crime?.let{
                        this.crime=crime
                        photoFile = crimeDetailViewModel.getPhotoFile(crime)
                        photoUri=FileProvider.getUriForFile(requireActivity(),"com.bignerdranch.kotlincriminalintent.fileprovider",photoFile)
                        updateUI()
                    }
        })
        parentFragmentManager.setFragmentResultListener(REQUEST_DATE, viewLifecycleOwner,this)
    }

    override fun onStart() {
        super.onStart()
        val titleWatcher = object : TextWatcher{
            /*
            This is a function belonging to TextWatcher which doesn't need implementation as of now
             */
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }
            /*
            This is a function belonging to TextWatcher which doesn't need implementation as of now
             */
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title=s.toString()
            }
            /*
            This is a function belonging to TextWatcher which doesn't need implementation as of now
             */
            override fun afterTextChanged(s: Editable?) {

            }
        }
        titleField.addTextChangedListener(titleWatcher)
        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }
        dateButton.setOnClickListener{
            DatePickerFragment
                .newInstance(crime.date, REQUEST_DATE)
                .show(parentFragmentManager, REQUEST_DATE)
        }
        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type="text/plain"
                putExtra(Intent.EXTRA_TEXT,getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT,getString(R.string.crime_report_subject))
            }.also{ intent ->
                val chooseIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooseIntent)
            }
        }
        suspectButton.apply {
            
            setOnClickListener {
                pickContactLauncher.launch(ContactsContract.Contacts.CONTENT_URI)
            }
        }
        photoButton.apply {
            val packageManager: PackageManager = requireActivity().packageManager
            val captureImage =  Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity: ResolveInfo? = packageManager.resolveActivity(captureImage,PackageManager.MATCH_DEFAULT_ONLY)
            if(resolvedActivity == null){
                isEnabled=false
            }
            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT,photoUri)
                val cameraActivities: List<ResolveInfo> = packageManager.queryIntentActivities(captureImage,PackageManager.MATCH_DEFAULT_ONLY)
                for(cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(cameraActivity.activityInfo.packageName,
                    photoUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                takePicture.launch(photoUri)
            }
        }
    }
    val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) {success:Boolean ->
        if(success){
            Log.d(TAG, "We took a pic..")
            updatePhotoView()
        }
    }
    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri,
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
    override fun onFragmentResult(requestKey: String, result: Bundle) {
        when(requestKey){
            REQUEST_DATE -> {
                Log.d(TAG,"received result for $requestKey")
                crime.date = DatePickerFragment.getSelectedDate(result)
                updateUI()
            }
        }
    }
    fun onDateSelected(date: Date) {
        crime.date=date
        updateUI()
    }

    private fun updateUI() {
        titleField.setText(crime.title)
        dateButton.text=crime.date.toString()
        solvedCheckBox.apply {
            isChecked=crime.isSolved
            jumpDrawablesToCurrentState()
        }
        if(crime.suspect.isNotEmpty()){
            suspectButton.text = crime.suspect
        }
        updatePhotoView()
    }
    private fun updatePhotoView(){
        if(photoFile.exists()){
            val bitmap = getScaledBitmap(photoFile.path, requireActivity())
            photoView.setImageBitmap(bitmap)
        }else {
            photoView.setImageDrawable(null)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG,"func Called")
        when{
            resultCode!=Activity.RESULT_OK -> return
            requestCode == REQUEST_CONTACT && data != null -> {
                val contactUri: Uri? = data.data
                val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
                val cursor = contactUri?.let { requireActivity().contentResolver.query(it,queryFields,null,null,null) }
                cursor?.use {
                    if(it.count == 0){
                        return
                    }
                    it.moveToFirst()
                    val suspect = it.getString(0)
                    crime.suspect = suspect
                    crimeDetailViewModel.saveCrime(crime)
                    suspectButton.text = suspect
                }
            }
            requestCode == REQUEST_PHOTO -> {
                requireActivity().revokeUriPermission(photoUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                updatePhotoView()
            }
        }
    }

    private fun getCrimeReport():String{
        val solvedString = if (crime.isSolved){
            getString(R.string.crime_report_solved)
        } else{
            getString(R.string.crime_report_unsolved)
        }
        val dateString = SimpleDateFormat(DATE_FORMAT,Locale.US).format(crime.date).toString()

        var suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        }else{
            getString(R.string.crime_report_suspect, crime.suspect)
        }
        return  getString(R.string.crime_report, crime.title, dateString,solvedString,suspect)
    }
    companion object {
        fun newInstance(crimeId: UUID) : CrimeFragment{
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments=args
            }
        }
    }
}