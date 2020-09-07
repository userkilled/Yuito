package net.accelf.yuito

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.entity.Instance
import com.mikepenz.materialdrawer.model.AbstractDrawerItem
import com.uber.autodispose.SingleSubscribeProxy
import kotlinx.android.synthetic.main.item_drawer_footer.view.*

class FooterDrawerItem : AbstractDrawerItem<FooterDrawerItem, FooterDrawerItem.ViewHolder>() {
    override val type = R.id.instanceData

    override val layoutRes = R.layout.item_drawer_footer

    private lateinit var context: Context
    private lateinit var instanceData: TextView

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        context = holder.itemView.context
        instanceData = holder.instanceData
        holder.itemView.setPadding(0, 0, 0, 0)
        instanceData.setTextColor(instanceData.hintTextColors)
    }

    override fun getViewHolder(v: View) = ViewHolder(v)

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
        internal val instanceData = view.instanceData
    }
}
