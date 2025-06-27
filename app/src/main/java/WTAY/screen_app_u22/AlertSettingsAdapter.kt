package WTAY.screen_app_u22

import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AlertSettingsAdapter(
    private val context: Context,
    private var settings: List<AlertSetting>,
    private val onDeleteClick: (AlertSetting) -> Unit
) : RecyclerView.Adapter<AlertSettingsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIcon: ImageView = view.findViewById(R.id.appIcon)
        val appName: TextView = view.findViewById(R.id.appName)
        val limitTime: TextView = view.findViewById(R.id.limitTime)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert_setting, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val setting = settings[position]
        holder.appName.text = setting.appName
        holder.limitTime.text = "上限: ${setting.limitInMinutes}分"

        try {
            holder.appIcon.setImageDrawable(context.packageManager.getApplicationIcon(setting.packageName))
        } catch (e: PackageManager.NameNotFoundException) {
            holder.appIcon.setImageResource(R.mipmap.ic_launcher)
        }

        holder.deleteButton.setOnClickListener {
            onDeleteClick(setting)
        }
    }

    override fun getItemCount() = settings.size

    fun updateData(newSettings: List<AlertSetting>) {
        settings = newSettings
        notifyDataSetChanged()
    }
}