package com.angelcry.preferredwifi

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.angelcry.preferredwifi.databinding.ItemNetworkBinding

class NetworkAdapter(
    private val preferredSsids: Set<String>,
    private val onPreferenceChanged: (ssid: String, isPreferred: Boolean) -> Unit
) : RecyclerView.Adapter<NetworkAdapter.ViewHolder>() {

    private var networks: List<String> = emptyList()

    fun submitList(list: List<String>) {
        networks = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNetworkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ssid = networks[position]
        holder.bind(ssid, preferredSsids.contains(ssid))
    }

    override fun getItemCount(): Int = networks.size

    inner class ViewHolder(private val binding: ItemNetworkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ssid: String, isPreferred: Boolean) {
            binding.tvSsid.text = ssid
            binding.switchPreferred.setOnCheckedChangeListener(null)
            binding.switchPreferred.isChecked = isPreferred

            binding.switchPreferred.setOnCheckedChangeListener { _, checked ->
                onPreferenceChanged(ssid, checked)
            }
        }
    }
}
