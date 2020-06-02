package com.example.voiceassistant.ui.main

import android.graphics.Canvas
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.voiceassistant.MainActivity

import com.example.voiceassistant.R
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.android.synthetic.main.frag2_fragment.view.*
import kotlinx.android.synthetic.main.my_text_view.view.*
import okio.Utf8
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class Frag2 : Fragment() {

    companion object {
        fun newInstance() = Frag2()
    }

    private lateinit var viewModel: Frag2ViewModel

    // declare my data set
    var myDataset:MutableList<String> = mutableListOf("")

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    inner class MyAdapter :
        RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder.
        // Each data item is just a string in this case that is shown in a TextView.
        inner class MyViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val date = view.date
            val name = view.name
            val ID = view.ID
            val speech = view.speech
        }


        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): MyAdapter.MyViewHolder {
            // create a new view

            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.my_text_view, parent, false)
            // set the view's size, margins, paddings and layout parameters
            return MyViewHolder(view)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element

            // make the latest speech showed first
            val new_position = myDataset.size-2-position
            val jsonObject = JSONObject(myDataset[new_position])
            holder.date.text = jsonObject.getString("date")
            holder.name.text = jsonObject.getString("name")
            holder.ID.text = jsonObject.getString("ID")
            holder.speech.text = "speech:" + jsonObject.getString("speech")
            /*holder.date.text = myDataset[position]
            holder.name.text = tmp
            holder.ID.text = "$flag123"*/
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = myDataset.size - 1
    }

    inner class ItemDragHelperCallback : ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            return makeMovementFlags(0, ItemTouchHelper.RIGHT)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            if(myDataset.size > 1) {
                return true
            }
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val realPosition = myDataset.size-2-position
            when(direction) {
                ItemTouchHelper.LEFT -> {

                }
                ItemTouchHelper.RIGHT -> {
                    myDataset.removeAt(realPosition)
                    viewAdapter.notifyItemRemoved(position)
                    writeDatasetToFile()
                }
            }

        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                .addBackgroundColor(ContextCompat.getColor(context!!, R.color.my_background))
                .addActionIcon(R.drawable.ic_garbage_can)
                .create()
                .decorate();
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    private fun readDatasetFromFile(): Int {
        // file's location
        val fileName = "${activity?.externalCacheDir?.absolutePath}/speechTextRecord"
        var inputFile: FileInputStream? = null

        try {
            inputFile = FileInputStream(fileName)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        // read file
        val text = ByteArray(2000)
        inputFile?.read(text,0,2000)!!

        myDataset = String(text).split("\n").toMutableList()
        //
        return myDataset.size
    }

    private fun writeDatasetToFile() {
        // file's location
        val fileName = "${activity?.externalCacheDir?.absolutePath}/speechTextRecord"
        var outputFile: FileOutputStream? = null

        try {
            outputFile = FileOutputStream(fileName)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        for(i in 0..myDataset.size-2) {
            val textLine = myDataset[i] + "\n"
            outputFile!!.write(textLine.toByteArray())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.frag2_fragment, container, false)

        readDatasetFromFile()

        viewManager = LinearLayoutManager(view.context)
        viewAdapter = MyAdapter()

        recyclerView = view.recyclerview.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            //setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter
        }

        // 下拉更新事件
        view.refreshLayout.setOnRefreshListener {
            val size = readDatasetFromFile()
            viewAdapter.notifyDataSetChanged()
            view.refreshLayout.isRefreshing = false
        }

        //設定左右滑可以刪除cell
        val helperCallback = ItemDragHelperCallback()
        val helper = ItemTouchHelper(helperCallback)
        helper.attachToRecyclerView(view.recyclerview)

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(Frag2ViewModel::class.java)
        // TODO: Use the ViewModel
    }

}
