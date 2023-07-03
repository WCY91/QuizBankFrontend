package com.example.yiquizapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.yiquizapp.BankRecyclerViewAdapter.MyViewHolder

class BankRecyclerViewAdapter(var context: Context, var bankModels: ArrayList<BankModel>) :
    RecyclerView.Adapter<MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
//        This is where you inflate the layout (Giving a look to our rows)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.recycler_view_row, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
//        Assigning values to the views we created in the recycler_view_row layout file
//        Based on the position of the recycler view
        holder.tv_BankName.text = bankModels[position].getBankName()
        holder.tv_BankDescription.text = bankModels[position].getBankDescription()
        holder.tv_BankDate.text = bankModels[position].getBankDate()
        //        holder.tv_BankImage.setImageResource(bankModels.get(position).getImage());
    }

    override fun getItemCount(): Int {
//        The recycler view just wants to know the number of items you want displayed
        return bankModels.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //        Grabbing the views from our recycler_view_row layout file
        //        Kinda like in the onCreate method
        var tv_BankImage: ImageView? = null
        var tv_BankName: TextView
        var tv_BankDescription: TextView
        var tv_BankDate: TextView

        init {

//            tv_BankImage = itemView.findViewById(R.id.bank_image);
            tv_BankName = itemView.findViewById(R.id.bank_name)
            tv_BankDescription = itemView.findViewById(R.id.bank_description)
            tv_BankDate = itemView.findViewById(R.id.bank_date)
        }
    }
}