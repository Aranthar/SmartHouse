package com.example.smarthouse

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthouse.constance.Const
import com.example.smarthouse.data.DataBaseManager
import com.example.smarthouse.model.Home
import java.util.*

class HomeAdapter(
    private val context: Context,
    private val dataset: MutableList<Home>
    ): RecyclerView.Adapter<HomeAdapter.HomeViewHolder>() {
    private val dataBaseManager = DataBaseManager(context)

    class HomeViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val homeTypeOfDwelling: TextView = view.findViewById(R.id.textHomeInfo)
        val homeRegion: TextView = view.findViewById(R.id.regionText)
        val homeCity: TextView = view.findViewById(R.id.cityText)
        val homeAddress: TextView = view.findViewById(R.id.addressText)
        val homeIndex: TextView = view.findViewById(R.id.indexText)
        val homeMoreInfo: ImageView = view.findViewById(R.id.iconMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        return HomeViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_home, parent, false))
    }

    @SuppressLint("InflateParams")
    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val item = dataset[position]

        holder.homeTypeOfDwelling.text = item.typeOfDwelling
        holder.homeRegion.text = item.region
        holder.homeCity.text = item.city
        holder.homeAddress.text = item.address
        holder.homeIndex.text = item.index

        holder.homeMoreInfo.setOnClickListener {
            showPopupMenu(item, holder, position, holder.homeMoreInfo)
        }
    }

    @SuppressLint("InflateParams")
    private fun showPopupMenu(item: Home, holder: HomeViewHolder, position: Int, view: View) {
        val popupMenu = PopupMenu(view.context, view)

        popupMenu.menu.add(0, Const.ID_EDIT_HOME, Menu.NONE, "Редактировать")
        popupMenu.menu.add(0, Const.ID_DELETE_HOME, Menu.NONE, "Удалить объект")

        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                Const.ID_EDIT_HOME -> {
                    dataBaseManager.openDataBase()
                    val builder = AlertDialog.Builder(context)
                    val inflater = LayoutInflater.from(context)
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
                                holder.homeTypeOfDwelling.text = item.typeOfDwelling
                                objectMassive.add(item.typeOfDwelling)
                            } else {
                                holder.homeTypeOfDwelling.text = typeOfDwelling.text.toString()
                                objectMassive.add(typeOfDwelling.text.toString())
                            }

                            if (regionEditText.text.toString() == "") {
                                holder.homeRegion.text = item.region
                                objectMassive.add(item.region)
                            } else {
                                holder.homeRegion.text = regionEditText.text.toString()
                                objectMassive.add(regionEditText.text.toString())
                            }

                            if (cityEditText.text.toString() == "") {
                                holder.homeCity.text = item.city
                                objectMassive.add(item.city)
                            } else {
                                holder.homeCity.text = cityEditText.text.toString()
                                objectMassive.add(cityEditText.text.toString())
                            }

                            if (addressEditText.text.toString() == "") {
                                holder.homeAddress.text = item.address
                                objectMassive.add(item.address)
                            } else {
                                holder.homeAddress.text = addressEditText.text.toString()
                                objectMassive.add(addressEditText.text.toString())
                            }

                            if (indexEditText.text.toString() == "") {
                                holder.homeIndex.text = item.index
                                objectMassive.add(item.index)
                            } else {
                                holder.homeIndex.text = indexEditText.text.toString()
                                objectMassive.add(indexEditText.text.toString())
                            }

                            updateDataBase("${Const.objectsTag}$position", objectMassive.joinToString("!!"))
                        }

                        setNegativeButton("Отмена") { _, _ ->
                            Log.d("Main", "Negative button clicked")
                        }

                        setView(dialogLayout)
                        show()
                    }
                }
                Const.ID_DELETE_HOME -> {
                    deleteObjectDB(position)
                }
            }
            return@setOnMenuItemClickListener true
        }

        popupMenu.show()
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    private fun updateDataBase(const: String, content: String) {
        val dataList = dataBaseManager.readDataBaseData()
        if (dataList.count { it[0] == const } != 0) {
            for (list in dataList) {
                if (list[0] == const) {
                    dataBaseManager.updateContent(const, content)
                }
            }
        } else dataBaseManager.insertToDataBase(const, content)
    }

    private fun deleteObjectDB(position: Int) {
        dataBaseManager.openDataBase()
        val dataList = dataBaseManager.readDataBaseData()

        if (position == dataset.size - 1) {
            dataBaseManager.deleteContent("${Const.objectsTag}${position}")
            dataset.removeAt(position)
        } else {
            var newPosition = position

            for (index in 0 until dataList.size - 1) {
                if (dataList[index][0] == "${Const.objectsTag}${newPosition}") {
                    val const = dataList[index][0].toString()
                    val content = dataList[index + 1][1]
                    updateDataBase(const, content!!)
                    newPosition++
                }
            }
            dataBaseManager.deleteContent("${Const.objectsTag}${dataset.size - 1}")
            dataset.removeAt(dataset.size - 1)
        }
    }
}