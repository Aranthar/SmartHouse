package com.example.smarthouse

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.smarthouse.constance.Const
import com.example.smarthouse.data.DataBaseManager
import com.example.smarthouse.data.DataSource
import com.example.smarthouse.databinding.ActivityMainBinding
import com.example.smarthouse.model.Home
import com.google.firebase.database.*
import me.relex.circleindicator.CircleIndicator3
import java.util.*
import kotlin.concurrent.timerTask

@Suppress("UNNECESSARY_SAFE_CALL")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ViewModel

//    Spinners
    private lateinit var spinnerActionMode: Spinner
    private lateinit var spinnerLightingSwitch: Spinner
    private lateinit var spinnerWindowMode: Spinner

//    DataBase
    private val dataBaseManager = DataBaseManager(this)
    private var dataSet = DataSource.houses
    private var dataSetSize = dataSet.size
    private lateinit var dbRef: DatabaseReference

    private lateinit var indicator: CircleIndicator3

//    For indicators
    private var indicatorCo2 = -1
    private var airHumidityIndex = -1
    private var soilMoistureIndex = -1
    private var actionMode = ""
    private var lightMode = ""
    private var actionWindowMode = ""
    private var isHomeLampOnPowerEnable = false
    private var isStreetLampOnPowerEnable = false
    private var windowIsOpen = false

//    Measuring range
    private val minIndicatorCo2 = 600
    private val maxIndicatorCo2 = 800
    private val minAirHumidity =30
    private val maxAirHumidity = 60
    private var minSoilMoisture = 60
    private var maxSoilMoisture = 70

    @SuppressLint("InflateParams", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbRef = FirebaseDatabase.getInstance().reference

        viewModel = ViewModelProvider(this)[ViewModel::class.java]

        viewModel.minCurrentNumber.observe(this) {
            minSoilMoisture = it
        }
        viewModel.maxCurrentNumber.observe(this) {
            maxSoilMoisture = it
        }

        viewModel.homeLampOnPower.observe(this) {
            isHomeLampOnPowerEnable = it
        }
        viewModel.streetLampOnPower.observe(this) {
            isStreetLampOnPowerEnable = it
        }

        if (viewModel.isHomeLampOnPowerEnable) {
            binding.imageLamp.setColorFilter(ContextCompat.getColor(this, R.color.light))
        } else {
            binding.imageLamp.setColorFilter(ContextCompat.getColor(this, R.color.white))
        }
        if (viewModel.isStreetLampOnPowerEnable) {
            binding.imageLamp.setColorFilter(ContextCompat.getColor(this, R.color.light))
        } else {
            binding.imageLamp.setColorFilter(ContextCompat.getColor(this, R.color.white))
        }

//        Flipping house screens
        binding.pager.adapter = HomeAdapter(this, dataSet)
        binding.pager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        indicator = findViewById(R.id.indicator)
        indicator.setViewPager(binding.pager)

//        Spinner action mode
        spinnerActionMode = binding.spinnerActivationMode
        val actionModeList = listOf("переключатель", "уровень освещения", "датчик движения")
        var arrayAdapter = ArrayAdapter(this, R.layout.spinner_right_aligned, actionModeList)
        arrayAdapter.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)

        spinnerActionMode.adapter = arrayAdapter
        spinnerActionMode.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                if (actionModeList[0] == p0?.getItemAtPosition(p2)) {
                    binding.icPower.visibility = View.VISIBLE

                    if (viewModel.isStreetLampOnPowerEnable) {
                        binding.imageLamp.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.light))
                    } else {
                        binding.imageLamp.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.white))
                    }

                    actionMode = "Switch"
                } else {
                    binding.icPower.visibility = View.GONE
                    binding.imageLamp.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.white))

                    deleteStringInDataBase(Const.powerStreetLightTag)
                    isStreetLampOnPowerEnable = false

                    actionMode = if (actionModeList[1] == p0?.getItemAtPosition(p2)) {
                        "LevelLighting"
                    } else "MotionSensor"

                }
                dbRef.child(Const.actionModeTag).setValue(actionMode)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
//                nothing
            }
        }

//        Spinner light mode
        spinnerLightingSwitch = binding.spinnerLightMode
        val lightModeList = listOf("домашнее", "уличное")
        arrayAdapter = ArrayAdapter(this, R.layout.spinner_right_aligned, lightModeList)
        arrayAdapter.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)

        spinnerLightingSwitch.adapter = arrayAdapter
        spinnerLightingSwitch.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                if (p0!!.getItemAtPosition(0) == p0.getItemAtPosition(p2)) {
                    lightMode = "homemade"
                    binding.spinnerActivationMode.visibility = View.GONE
                    binding.textModeActivation.visibility = View.GONE
                    binding.icPower.visibility = View.VISIBLE
                } else {
                    lightMode = "street"
                    binding.spinnerActivationMode.visibility = View.VISIBLE
                    binding.textModeActivation.visibility = View.VISIBLE
                }
                dbRef.child(Const.lightModeTag).setValue(lightMode)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
//                nothing
            }
        }

//        Spinner window open mode
        spinnerWindowMode = binding.spinnerWindowOpen
        val windowModeList = listOf("50%", "100%")
        arrayAdapter = ArrayAdapter(this, R.layout.spinner_right_aligned, windowModeList)
        arrayAdapter.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)

        spinnerWindowMode.adapter = arrayAdapter
        spinnerWindowMode.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                actionWindowMode = if (p0!!.getItemAtPosition(0) == p0.getItemAtPosition(p2)) {
                    "50"
                } else "100"
                dbRef.child(Const.windowOpenModeTag).setValue(actionWindowMode)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
//                nothing
            }
        }

//        Light switch
        binding.icPower.setOnClickListener {
            if (lightMode == "homemade") {
                if (isHomeLampOnPowerEnable) {
                    binding.imageLamp.setColorFilter(ContextCompat.getColor(this, R.color.white))
                    viewModel.isHomeLampOnPowerEnable = false
                    viewModel.homeLampOnPower.value = viewModel.isHomeLampOnPowerEnable

                    dbRef.child(Const.powerHomeLightTag).setValue("false")
                } else {
                    binding.imageLamp.setColorFilter(ContextCompat.getColor(this, R.color.light))
                    viewModel.isHomeLampOnPowerEnable = true
                    viewModel.homeLampOnPower.value = viewModel.isHomeLampOnPowerEnable

                    dbRef.child(Const.powerHomeLightTag).setValue("true")
                }
            } else {
                if (isStreetLampOnPowerEnable) {
                    binding.imageLamp.setColorFilter(ContextCompat.getColor(this, R.color.white))
                    viewModel.isStreetLampOnPowerEnable = false
                    viewModel.streetLampOnPower.value = viewModel.isStreetLampOnPowerEnable

                    dbRef.child(Const.powerStreetLightTag).setValue("false")
                } else {
                    binding.imageLamp.setColorFilter(ContextCompat.getColor(this, R.color.light))
                    viewModel.isStreetLampOnPowerEnable = true
                    viewModel.streetLampOnPower.value = viewModel.isStreetLampOnPowerEnable

                    dbRef.child(Const.powerStreetLightTag).setValue("true")
                }
            }
        }

//        Window
        binding.icPowerWindow.setOnClickListener {
            if (windowIsOpen) {
                binding.textOpen.visibility = View.VISIBLE
                binding.spinnerWindowOpen.visibility = View.VISIBLE
                binding.textClose.visibility = View.GONE

                windowIsOpen = false

                dbRef.child(Const.windowPowerTag).setValue(windowIsOpen)
            } else {
                binding.textOpen.visibility = View.GONE
                binding.spinnerWindowOpen.visibility = View.GONE
                binding.textClose.visibility = View.VISIBLE

                windowIsOpen = true

                dbRef.child(Const.windowPowerTag).setValue(windowIsOpen)
            }
        }

//        Toast info
        binding.icInfoCo2.setOnClickListener {
            Toast.makeText(this, "Диапазон допустимой нормы 600-800 ppm.", Toast.LENGTH_SHORT).show()
            Log.d("ANIME", dataSet.joinToString(" "))
        }
        binding.icInfoWaterSoil.setOnClickListener {
            Toast.makeText(this, "Диапазон допустимой нормы $minSoilMoisture-$maxSoilMoisture%.", Toast.LENGTH_SHORT).show()
        }
        binding.icInfoWaterHome.setOnClickListener {
            Toast.makeText(this, "Диапазон допустимой нормы 30-60%.", Toast.LENGTH_SHORT).show()
        }

//        Soil moisture min max edit
        binding.icEditSoilMoisture.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val inflater = LayoutInflater.from(this)
            val dialogLayout = inflater.inflate(R.layout.soil_moisture_edit, null)

            val minSoilMoistureEdit = dialogLayout.findViewById<EditText>(R.id.minSoilMoisture)
            val maxSoilMoistureEdit = dialogLayout.findViewById<EditText>(R.id.maxSoilMoisture)
            var error = 0

            with(builder) {
                setTitle("Введите диапазон значений")
                setPositiveButton("Применить") { _, _ ->
                    if (minSoilMoistureEdit.text.toString() == "" ||
                        minSoilMoistureEdit.text.toString().toInt() > 100) {

                        error--
                    } else {
                        viewModel.minNumber = minSoilMoistureEdit.text.toString().toInt()
                        viewModel.minCurrentNumber.value = viewModel.minNumber
                    }

                    if (maxSoilMoistureEdit.text.toString() == "" ||
                        maxSoilMoistureEdit.text.toString().toInt() > 100) {

                        error--
                    } else {
                        viewModel.maxNumber = maxSoilMoistureEdit.text.toString().toInt()
                        viewModel.maxCurrentNumber.value = viewModel.maxNumber
                    }

                    if (minSoilMoisture > maxSoilMoisture) {
                        Toast.makeText(this@MainActivity, "Минимальное значение не может быть больше максимального.", Toast.LENGTH_SHORT).show()
                    }

                    if (error < 0) {
                        Toast.makeText(this@MainActivity, "Одно или несколько значений было введено неверно.", Toast.LENGTH_SHORT).show()
                    }

                    if (error == 0 && minSoilMoisture < maxSoilMoisture) {
                        dbRef.child(Const.minSoilMoistureTag).setValue(viewModel.minNumber.toString())
                        dbRef.child(Const.maxSoilMoistureTag).setValue(viewModel.maxNumber.toString())
                    }
                    error = 0
                }

                setNegativeButton("Отмена") { _, _ ->
                    Log.d("Main", "Negative button clicked")
                }
                setView(dialogLayout)
                show()
            }
        }
    }

//    Menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    @SuppressLint("InflateParams")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home_plus -> {
                dataBaseManager.openDataBase()
                val builder = AlertDialog.Builder(this)
                val inflater = LayoutInflater.from(this)
                val dialogLayout = inflater.inflate(R.layout.home_edit_layout, null)
                val objectMassive = mutableListOf<String>()

                val typeOfDwelling = dialogLayout.findViewById<EditText>(R.id.home_type_editText)
                val regionEditText = dialogLayout.findViewById<EditText>(R.id.region_editText)
                val cityEditText = dialogLayout.findViewById<EditText>(R.id.city_editText)
                val addressEditText = dialogLayout.findViewById<EditText>(R.id.address_editText)
                val indexEditText = dialogLayout.findViewById<EditText>(R.id.index_editText)

                with(builder) {
                    setTitle("Введите параметры объекта")
                    setPositiveButton("Применить") { _, _ ->
                        if (typeOfDwelling.text.toString() == "") {
                            objectMassive.add(Const.NaN)
                        } else {
                            objectMassive.add(typeOfDwelling.text.toString())
                        }

                        if (regionEditText.text.toString() == "") {
                            objectMassive.add(Const.NaN)
                        } else {
                            objectMassive.add(regionEditText.text.toString())
                        }

                        if (cityEditText.text.toString() == "") {
                            objectMassive.add(Const.NaN)
                        } else {
                            objectMassive.add(cityEditText.text.toString())
                        }

                        if (addressEditText.text.toString() == "") {
                            objectMassive.add(Const.NaN)
                        } else {
                            objectMassive.add(addressEditText.text.toString())
                        }

                        if (indexEditText.text.toString() == "") {
                            objectMassive.add(Const.NaN)
                        } else {
                            objectMassive.add(indexEditText.text.toString())
                        }
                        updateDataBase("${Const.objectsTag}${dataSet.size}", objectMassive.joinToString("!!"))
                    }

                    setNegativeButton("Отмена") { _, _ ->
                        Log.d("Main", "Negative button clicked")
                    }

                    setView(dialogLayout)
                    show()
                }
            }
        }

        return true
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("SetTextI18n", "UnspecifiedImmutableFlag", "NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        dataBaseManager.openDataBase()

//        Data update
        val timer = Timer()
        val handler = Handler()

        timer.schedule(timerTask {
            kotlin.run {
                dbRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (empSnap in snapshot.children) {
                                when (empSnap.key) {
                                    Const.currentCo2Tag -> continue
                                    Const.currentAirHumidityTag -> continue
                                    Const.currentSoilMoistureTag -> continue
                                    else -> updateDataBase(empSnap.key.toString(), empSnap.value.toString())
                                }
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {

                    }
                })

                val dataList = dataBaseManager.readDataBaseData()

                for (list in dataList) {
                    when(list[0]) {
                        Const.powerHomeLightTag -> isHomeLampOnPowerEnable = list[1]?.toBoolean()!!
                        Const.powerStreetLightTag -> isStreetLampOnPowerEnable = list[1]?.toBoolean()!!
                        Const.windowPowerTag -> windowIsOpen = list[1]?.toBoolean()!!
                        Const.minSoilMoistureTag -> minSoilMoisture = list[1]?.toInt()!!
                        Const.maxSoilMoistureTag -> maxSoilMoisture = list[1]?.toInt()!!
                        Const.actionModeTag -> actionMode = list[1]!!
                        Const.lightModeTag -> lightMode = list[1]!!
                        Const.windowOpenModeTag -> actionWindowMode = list[1]!!
                    }
                    if (list[0]?.matches("${Const.objectsTag}\\d".toRegex())!!) {
                        val position = list[0]?.replace(Const.objectsTag, "")!!.toInt()
                        val content = list[1]?.split("!!")

                        if (position == dataSet.size) {
                            dataSet.add(
                                Home(
                                    content?.get(0) ?: Const.NaN,
                                    content?.get(1) ?: Const.NaN,
                                    content?.get(2) ?: Const.NaN,
                                    content?.get(3) ?: Const.NaN,
                                    content?.get(4) ?: Const.NaN)
                            )

                            handler.post {
                                kotlin.run {
                                    binding.pager.adapter?.notifyItemChanged(binding.pager.adapter!!.itemCount - 1)
                                    indicator.setViewPager(binding.pager)
                                }
                            }
                        } else {
                            dataSet[position] = Home(
                                content?.get(0) ?: Const.NaN,
                                content?.get(1) ?: Const.NaN,
                                content?.get(2) ?: Const.NaN,
                                content?.get(3) ?: Const.NaN,
                                content?.get(4) ?: Const.NaN)
                        }
                    }
                }

                handler.post {
                    kotlin.run {
                        binding.pagerNull?.visibility = if (dataSet.size == 0) View.VISIBLE else View.GONE

                        if (dataSetSize != dataSet.size) {
                            binding.pager.adapter?.notifyDataSetChanged()
                            indicator.setViewPager(binding.pager)
                            dataSetSize = dataSet.size
                        }

                        if (lightMode == "homemade") {
                            if (isHomeLampOnPowerEnable) {
                                binding.imageLamp.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.light))
                            } else {
                                binding.imageLamp.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.white))
                            }
                        } else {
                            if (isStreetLampOnPowerEnable) {
                                binding.imageLamp.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.light))
                            } else {
                                binding.imageLamp.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.white))
                            }
                        }

                        if (windowIsOpen) {
                            binding.textOpen.visibility = View.GONE
                            binding.spinnerWindowOpen.visibility = View.GONE
                            binding.textClose.visibility = View.VISIBLE
                        } else {
                            binding.textOpen.visibility = View.VISIBLE
                            binding.spinnerWindowOpen.visibility = View.VISIBLE
                            binding.textClose.visibility = View.GONE
                        }

                        when (actionMode) {
                            "Switch" -> spinnerActionMode.setSelection(0)
                            "LevelLighting" -> spinnerActionMode.setSelection(1)
                            "MotionSensor" -> spinnerActionMode.setSelection(2)
                        }

                        when (lightMode) {
                            "homemade" -> spinnerLightingSwitch.setSelection(0)
                            "street" -> spinnerLightingSwitch.setSelection(1)
                        }

                        when (actionWindowMode) {
                            "50" -> spinnerWindowMode.setSelection(0)
                            "100" -> spinnerWindowMode.setSelection(1)
                        }
                    }
                }
            }
        }, 0L, 10L * 100)

        timer.schedule(timerTask {
            kotlin.run {
                dbRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (empSnap in snapshot.children) {
                                when (empSnap.key) {
                                    Const.currentCo2Tag -> updateDataBase(empSnap.key.toString(), empSnap.value.toString())
                                    Const.currentAirHumidityTag -> updateDataBase(empSnap.key.toString(), empSnap.value.toString())
                                    Const.currentSoilMoistureTag -> updateDataBase(empSnap.key.toString(), empSnap.value.toString())
                                }
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {

                    }
                })

                val dataList = dataBaseManager.readDataBaseData()

                for (list in dataList) {
                    when(list[0]) {
                        Const.currentCo2Tag -> indicatorCo2 = list[1]?.toInt()!!
                        Const.currentAirHumidityTag -> airHumidityIndex = list[1]?.toInt()!!
                        Const.currentSoilMoistureTag -> soilMoistureIndex = list[1]?.toInt()!!
                    }
                }

                handler.post {
                    kotlin.run {
                        when (indicatorCo2) {
                            -1 -> {
                                binding.meaningCo2.text = "0"
                                binding.co2Text.text = Const.NaN
                            }
                            else -> {
                                binding.meaningCo2.text = indicatorCo2.toString()

                                when {
                                    indicatorCo2 in minIndicatorCo2..maxIndicatorCo2 -> binding.co2Text.text =
                                        Const.normal
                                    indicatorCo2 < minIndicatorCo2 -> binding.co2Text.text =
                                        Const.belowNormal
                                    else -> binding.co2Text.text =
                                        Const.aboveTheNorm
                                }
                            }
                        }

                        when (airHumidityIndex) {
                            -1 -> {
                                binding.airHumidityValue.text  = "0%"
                                binding.airHumidityText.text = Const.NaN
                            }
                            else -> {
                                binding.airHumidityValue.text  = "$airHumidityIndex%"

                                when {
                                    airHumidityIndex in minAirHumidity..maxAirHumidity -> binding.airHumidityText.text =
                                        Const.normal
                                    airHumidityIndex < minAirHumidity -> binding.airHumidityText.text =
                                        Const.belowNormal
                                    else -> binding.airHumidityText.text =
                                        Const.aboveTheNorm
                                }
                            }
                        }

                        when (soilMoistureIndex) {
                            -1 -> {
                                binding.soilMoistureValue.text  = "0%"
                                binding.soilMoistureText.text = Const.NaN
                            }
                            else -> {
                                binding.soilMoistureValue.text  = "$soilMoistureIndex%"

                                when {
                                    soilMoistureIndex in minSoilMoisture..maxSoilMoisture -> binding.soilMoistureText.text =
                                        Const.normal
                                    soilMoistureIndex < minSoilMoisture -> binding.soilMoistureText.text =
                                        Const.belowNormal
                                    else -> binding.soilMoistureText.text =
                                        Const.aboveTheNorm
                                }
                            }
                        }
                    }
                }
            }
        }, 0L, 5L * 1000)
    }

    fun updateDataBase(const: String, content: String) {
        val dataList = dataBaseManager.readDataBaseData()
        if (dataList.count { it[0] == const } != 0) {
            for (list in dataList) {
                if (list[0] == const) {
                    dataBaseManager.updateContent(const, content)
                }
            }
        } else dataBaseManager.insertToDataBase(const, content)
    }

    fun deleteStringInDataBase(type: String) {
        dataBaseManager.deleteContent(type)
    }
}