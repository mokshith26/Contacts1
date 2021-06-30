package com.swing.contacts

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.swing.contacts.adapters.ContactsAdapter
import com.swing.contacts.common.ViewUtils
import com.swing.contacts.models.ContactsResults
import kotlinx.android.synthetic.main.add_contact_pop_up.view.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private var adapter: ContactsAdapter? = null
    private var contactsList: MutableList<ContactsResults?> = mutableListOf()
    private val permissions = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS
    )
    private var job: Job? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        checkPermissions()

        adapter = ContactsAdapter(this, contactsList)
        val mLayoutManager = LinearLayoutManager(applicationContext)
        recyclerViewMainActivity.layoutManager = mLayoutManager
        recyclerViewMainActivity.itemAnimator = DefaultItemAnimator()
        recyclerViewMainActivity.adapter = adapter
    }

    private fun someMethod() {
        val result =
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CONTACTS)
        if (result == PackageManager.PERMISSION_DENIED) {
            checkPermissions()
        } else {
            recyclerViewMainActivity.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            job = CoroutineScope(Dispatchers.IO).launch {
                loadContacts()
            }
        }
    }

    private suspend fun loadContacts() {
        contactsList.clear()
        val contentResolver = contentResolver
        val cursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.Data.DISPLAY_NAME_PRIMARY
        )
        if (cursor != null && cursor.count > 0) {
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID))
                val name =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME_PRIMARY))
                val hasPhoneNumber =
                    cursor.getInt(cursor.getColumnIndex(ContactsContract.Data.HAS_PHONE_NUMBER))
                val phoneNumbers = ArrayList<String>()
                var phoneNumber = ""
                if (hasPhoneNumber > 0) {
                    val phoneCursor = contentResolver.query(
                        ContactsContract.Data.CONTENT_URI,
                        null,
                        "${ContactsContract.Data.CONTACT_ID} = ?",
                        arrayOf(id),
                        null
                    )
                    phoneCursor!!.moveToFirst()
//                    while (!phoneCursor.isAfterLast) {
                    val type: Int =
                        cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE))

                    if (type == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) {
                        phoneNumber = cursor.getString(
                            cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        );
                    }
                    phoneNumbers.add(phoneNumber)
                    phoneCursor.moveToNext()

                    phoneCursor.close()
                }
                if (phoneNumber.isNotEmpty()) {
                    val contList = ContactsResults(name, phoneNumber)
                    //Log.e("TAG", "$phoneNumber  ,     $name")
                    contactsList.add(contList)
                }
            }
            cursor.close()
            if (contactsList.isNotEmpty()) {
                val distinct: MutableList<ContactsResults?> =
                    LinkedHashSet(contactsList).toMutableList()
                contactsList.clear()
                contactsList.addAll(distinct)
            }

            withContext(Dispatchers.Main) {

                contactsList.sortWith { lhs, rhs -> lhs!!.name.compareTo(rhs!!.name) }
                progressBar.visibility = View.GONE
                recyclerViewMainActivity.visibility = View.VISIBLE
                adapter?.notifyDataSetChanged()
            }
        } else {
            withContext(Dispatchers.Main) {
                recyclerViewMainActivity.visibility = View.GONE
                progressBar.visibility = View.GONE
                notContactsText.visibility = View.VISIBLE
                Toast.makeText(
                    applicationContext,
                    getString(R.string.no_contacts),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun checkPermissions() {
        var result: Int
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        for (p in permissions) {
            result = ContextCompat.checkSelfPermission(applicationContext, p)
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p)
            }
        }
        if (listPermissionsNeeded.isNotEmpty()) {
            val MULTIPLE_PERMISSIONS = 10
            ActivityCompat.requestPermissions(
                this,
                listPermissionsNeeded.toTypedArray(),
                MULTIPLE_PERMISSIONS
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                if (job!!.isActive) {
                    job!!.cancel()
                }
                showContactPopUp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showContactPopUp() {
        val mDialogView =
            LayoutInflater.from(this).inflate(R.layout.add_contact_pop_up, null)
        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)
            .setTitle(R.string.add_contact)
        val mAlertDialog = mBuilder.show()
        mDialogView.saveTextPopUpContact.setOnClickListener {
            if (mDialogView.contactNameTextPopUpContact.text!!.isEmpty()) {
                ViewUtils.showToast(this, "Enter Name")
                return@setOnClickListener
            }
            if (mDialogView.contactNumberTextPopUpContact.text!!.isEmpty()) {
                ViewUtils.showToast(this, "Enter Number")
                return@setOnClickListener
            }
            val intent = Intent(Intent.ACTION_INSERT)
            intent.type = ContactsContract.RawContacts.CONTENT_TYPE
            intent.putExtra(
                ContactsContract.Intents.Insert.NAME,
                mDialogView.contactNameTextPopUpContact.text.toString()
            )
            intent.putExtra(
                ContactsContract.Intents.Insert.PHONE,
                mDialogView.contactNumberTextPopUpContact.text.toString()
            )
            startActivity(intent)
            mAlertDialog.dismiss()
        }
        mDialogView.cancelTextPopUpContact.setOnClickListener {
            mAlertDialog.dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        if (job == null || !job!!.isActive) {
            someMethod()
        }
    }
}