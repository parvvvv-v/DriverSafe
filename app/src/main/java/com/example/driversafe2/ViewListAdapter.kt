package com.example.driversafe2

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.driversafe2.databinding.DocumentlistBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
class ViewListAdapter(
    private val context: Context,
    private val databaseReference: DatabaseReference,
    private val menulist: ArrayList<AllMenu>,
    private val onDeleteClickListenner:(position:Int)->Unit
):RecyclerView.Adapter<ViewListAdapter.ViewListViewHolder>(){
    inner class ViewListViewHolder(private val binding: DocumentlistBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(position:Int) {
            val menuitem = menulist[position]
            binding.viewidtype.text =menuitem.idType?:"No Type"
            binding.viewexpiry.text = menuitem.expiryDate?:"No Expiry"
            if(!menuitem.imageUrl.isNullOrEmpty()) {
                Glide.with(context).load(menuitem.imageUrl).into(binding.idimage)
            } else {
                binding.idimage.setImageResource(android.R.color.black)
            }
            binding.deletebutton.setOnClickListener {
                onDeleteClickListenner(position)
            }
            binding.idimage.setOnClickListener {
                showZoomDialog(menuitem.imageUrl)
            }
        }

    private fun showZoomDialog(imageUrl: String?) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_zoom_image, null)
        val zoomImageView = dialogView.findViewById<com.github.chrisbanes.photoview.PhotoView>(R.id.zoomedImageView)
        Glide.with(context).load(imageUrl).into(zoomImageView)
        val dialog = android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(dialogView)
        zoomImageView.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewListViewHolder {
        val binding = DocumentlistBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewListViewHolder(binding)
    }

    override fun getItemCount(): Int = menulist.size
    override fun onBindViewHolder(holder: ViewListViewHolder, position: Int) {
        holder.bind(position)
    }
}