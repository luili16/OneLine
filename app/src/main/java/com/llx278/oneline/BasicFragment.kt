package com.llx278.oneline

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_basic.*

class BasicFragment : Fragment() {

    companion object {
        fun getInstance(): BasicFragment {
            return BasicFragment()
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        Log.d("main", "onAttach()")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d("main", "onCreateView()")
        return inflater.inflate(R.layout.fragment_basic, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("main", "onViewCreated() id is ${basic_recycler_container.id}")

        val gameDataSet: Array<GameInfo> = Array(10) {
            GameInfo(1, 2, 50, false)
        }

        val viewAdapter = MyAdapter(gameDataSet)
        val viewLayoutManager = LinearLayoutManager(view.context)
        basic_recycler_container.apply {
            layoutManager = viewLayoutManager
            adapter = viewAdapter
            val space = context!!.resources.getDimension(R.dimen.space_item_decoration)
            addItemDecoration(SpaceItemDecoration(space.toInt()))
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d("main", "onActivityCreated()")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("main", "onDestroyView()")
    }

    override fun onDetach() {
        super.onDetach()
        Log.d("main", "onDetach()")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("main", "onDestroy()")
    }

    private inner class MyViewHolder(private val myItemView: View) : RecyclerView.ViewHolder(myItemView) {

        val levelView: TextView = myItemView.findViewById(R.id.game_level)
        val clearView: TextView = myItemView.findViewById(R.id.clear_count)
    }

    private inner class MyAdapter(private val gameDataSet: Array<GameInfo>) : RecyclerView.Adapter<MyViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.colorful_board,
                    parent, false)
            return MyViewHolder(itemView)
        }

        override fun getItemCount(): Int = gameDataSet.size

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val gameInfo = gameDataSet[position]
            holder.levelView.text = gameInfo.level.toString()
            val clearStr = gameInfo.finishCount.toString() + "/" + gameInfo.totalCount.toString()
            holder.clearView.text = clearStr
        }
    }

    private inner class SpaceItemDecoration(val space: Int) : RecyclerView.ItemDecoration() {

        private val paint = Paint()

        init {
            paint.color = Color.RED
            paint.style = Paint.Style.FILL
        }

        override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
            outRect!!.left = space
            outRect.right = space
            outRect.bottom = space
            outRect.top = space
        }

        override fun onDrawOver(c: Canvas?, parent: RecyclerView?, state: RecyclerView.State?) {
            super.onDrawOver(c, parent, state)


            //c!!.drawRect(0f, 0f, space, c.width.toFloat(), paint)

        }

        override fun onDraw(c: Canvas?, parent: RecyclerView?, state: RecyclerView.State?) {
            super.onDraw(c, parent, state)
            c!!.drawCircle(0f, 0f, 50f, paint)
        }
    }
}