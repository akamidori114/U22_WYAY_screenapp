package WTAY.screen_app_u22

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class AlertSettingsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var emptyView: TextView
    private lateinit var adapter: AlertSettingsAdapter
    private lateinit var dataStore: AppDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert_settings)

        dataStore = AppDataStore(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView = findViewById(R.id.recyclerView)
        fab = findViewById(R.id.fab)
        emptyView = findViewById(R.id.emptyView)

        setupRecyclerView()

        fab.setOnClickListener {
            showSetLimitDialog("com.google.android.youtube", "YouTube")
        }

        observeSettings()
    }

    private fun setupRecyclerView() {
        adapter = AlertSettingsAdapter(this, emptyList()) { setting ->
            lifecycleScope.launch {
                dataStore.removeAlertSetting(setting.packageName)
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun observeSettings() {
        lifecycleScope.launch {
            dataStore.getAlertSettingsFlow().collect { settings ->
                adapter.updateData(settings)
                if (settings.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                }
            }
        }
    }

    private fun showSetLimitDialog(packageName: String, appName: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_set_limit, null)
        val editText = dialogView.findViewById<EditText>(R.id.limitEditText)

        AlertDialog.Builder(this)
            .setTitle("$appName の上限時間設定")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val limit = editText.text.toString().toLongOrNull() ?: 60
                val newSetting = AlertSetting(packageName, appName, limit)
                lifecycleScope.launch {
                    dataStore.addOrUpdateAlertSetting(newSetting)
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}