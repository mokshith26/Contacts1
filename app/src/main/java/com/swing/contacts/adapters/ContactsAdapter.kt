package com.swing.contacts.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.swing.contacts.R
import com.swing.contacts.models.ContactsResults
import kotlinx.android.synthetic.main.contacts_adaptor.view.*

class ContactsAdapter(val activity: Activity, private val contactsList: MutableList<ContactsResults?>) : RecyclerView.Adapter<ContactsAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ContactsAdapter.MyViewHolder {
        val itemView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.contacts_adaptor, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ContactsAdapter.MyViewHolder, position: Int) {
        holder.itemView.nameTextContactsAdapter.text = contactsList[position]!!.name
        holder.itemView.numberTextContactsAdapter.text = contactsList[position]!!.number
    }

    override fun getItemCount(): Int {
       return contactsList.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
        }
    }
}