/*
 * Copyleft (/c) MMXV, Proxy.sh
 * Distributed under the GNU GPL v2
 */
package com.proxysh.shieldtra;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.proxy.sh.shieldtra.R;

public class ProtoListAction implements OnItemClickListener {

    private AppActivity owner;
    private ListView listProto;
    private ProtoListAdapter listAdapter;

    public ProtoListAction(View v, AppActivity owner) {
        this.owner = owner;
        this.listProto = (ListView) v.findViewById(R.id.listProto);
        this.listProto.setOnItemClickListener(this);
        this.listAdapter = new ProtoListAdapter(owner);
        this.listProto.setAdapter(listAdapter);
    }

    public void clearActiveService() {

        if (listAdapter == null)
            return;

        if (listAdapter.activePos != -1) {
            int old = listAdapter.activePos;
            listAdapter.activePos = -1;
            listProto.getAdapter().getView(old,
                    null, listProto);

        }
    }

    public void markActiveService(String proto, String port) {

        if (listAdapter == null)
            return;

        int i = 0;
        for (i = 0; i < listAdapter.MAX_DEFINED_POROT; i++) {
            if (proto.equals(listAdapter.DEFINED_PROTO_BY_PORT[i]) &&
                    port.equals(listAdapter.DEFINED_PORT_LIST[i])) {
                break;
            }
        }
        if (i < listAdapter.MAX_DEFINED_POROT) {
            if (i != listAdapter.activePos) {
                clearActiveService();
                listAdapter.activePos = i;
                listProto.getAdapter().getView(i, null, listProto);
            }
        } else {
            clearActiveService();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View v, int pos, long arg3) {
        // TODO Auto-generated method stub
        listAdapter.setSelectItem(pos);
        owner.onSelectService(listAdapter.DEFINED_PROTO_BY_PORT[pos], listAdapter.DEFINED_PORT_LIST[pos]);
    }
}

class ProtoListAdapter extends BaseAdapter {

    private Context c;
    private int itemIndex = -1;
    public int activePos = -1;
    public final int MAX_DEFINED_POROT = 10;
    public final String DEFINED_PROTO_BY_PORT[] = {
            "TLSCrypt TCP",
            "TLSCrypt UDP",
            "TLSCrypt TCP",
            "TLSCrypt UDP",
            "TLSCrypt TCP",
            "TLSCrypt UDP",
            "TLSCrypt + XOR UDP",
            "TLSCrypt + XOR TCP",
            "TLSCrypt + XOR UDP",
            "TLSCrypt + XOR TCP",
    };
    public final String DEFINED_PORT_LIST[] = {
            "1194",
            "1194",
            "8080",
            "8080",
            "8181",
            "8181",
            "9898",
            "9898",
            "9090",
            "9090"
    };

    public ProtoListAdapter(Context c) {
        this.c = c;
    }

    public int getCount() {
        // TODO Auto-generated method stub
        return MAX_DEFINED_POROT;
    }

    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) c
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.port_item_row, null);

            holder = new ViewHolder();
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.textItemProto = (TextView) convertView.findViewById(R.id.textItemProto);
        holder.imageGo = (ImageView) convertView.findViewById(R.id.imageMark);

//		int imageId;
//		int colorId;
//		if (activePos == position || itemIndex == position) {
//			imageId = R.drawable.ic_item_go_active;
//			colorId = c.getResources().getColor(R.color.ActionTextColor);
//			holder.textItemProto.setTextColor(colorId);
//			holder.imageGo.setImageResource(imageId);
//		}

        holder.textItemProto.setText(DEFINED_PROTO_BY_PORT[position] + " " + DEFINED_PORT_LIST[position]);
        return convertView;
    }

    public void setSelectItem(int index) {
        itemIndex = index;
    }

    public int getSelectItem() {
        return itemIndex;
    }

    static class ViewHolder {
        TextView textItemProto;
        ImageView imageGo;
    }
}

