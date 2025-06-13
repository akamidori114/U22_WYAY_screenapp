package WTAY.screen_app_u22

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.TimeUnit

class UsageListAdapter(private val context: Context, private val usageStatsList: List<AppUsageDisplayItem>) :
    RecyclerView.Adapter<UsageListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIconImageView: ImageView = view.findViewById(R.id.appIconImageView)
        val appNameTextView: TextView = view.findViewById(R.id.textViewAppName)
        val packageNameTextView: TextView = view.findViewById(R.id.textViewPackageName)
        val usageTimeTextView: TextView = view.findViewById(R.id.textViewUsageTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usage_stat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = usageStatsList[position]
        holder.appNameTextView.text = item.appName
        holder.packageNameTextView.text = item.packageName
        holder.usageTimeTextView.text = formatMillisToHoursMinutes(item.totalTimeInForeground)

        try {
            val icon: Drawable = context.packageManager.getApplicationIcon(item.packageName)
            holder.appIconImageView.setImageDrawable(icon)
        } catch (e: PackageManager.NameNotFoundException) {
            holder.appIconImageView.setImageResource(R.mipmap.ic_launcher) // Default icon
        }
    }

    override fun getItemCount() = usageStatsList.size

    private fun formatMillisToHoursMinutes(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return when {
            hours > 0 -> "${hours}時間 ${minutes}分"
            minutes > 0 -> "${minutes}分"
            millis > 0 -> "< 1分"
            else -> "0分"
        }
    }
}