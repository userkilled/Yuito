package net.accelf.yuito

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.appstore.DrawerFooterClickedEvent
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.entity.Instance
import com.mikepenz.materialdrawer.model.AbstractDrawerItem
import com.uber.autodispose.SingleSubscribeProxy

class FooterDrawerItem : AbstractDrawerItem<FooterDrawerItem, FooterDrawerItem.ViewHolder>() {
    override val type: Int
        get() = R.id.instanceData

    override val layoutRes: Int
        get() = R.layout.item_drawer_footer

    lateinit var eventHub: EventHub

    private lateinit var context: Context
    private lateinit var instanceData: TextView

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        context = holder.itemView.context
        instanceData = holder.instanceData
        holder.itemView.setPadding(0, 0, 0, 0)
        instanceData.setTextColor(instanceData.hintTextColors)
        onDrawerItemClickListener = { _, _, _ ->
            var result = true
            var text = instanceData.text.toString()
            text += "?"
            if (text.endsWith("???????")) {
                text = text.substring(0, text.length - 7)
                eventHub.dispatch(DrawerFooterClickedEvent(true))
                isExpanded = false
                result = false
            }
            instanceData.text = text
            result
        }
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    fun setSubscribeProxy(subscribeProxy: SingleSubscribeProxy<Instance>) {
        subscribeProxy.subscribe(
                { instance ->
                    instanceData.text = String.format("%s\n%s\n%s", instance.title, instance.uri, instance.version)
                },
                {
                    instanceData.text = context.getString(R.string.instance_data_failed)
                }
        )
    }

    class ViewHolder internal constructor(internal val view: View): RecyclerView.ViewHolder(view) {
        internal val instanceData: TextView = view.findViewById(R.id.instanceData)
    }
}
