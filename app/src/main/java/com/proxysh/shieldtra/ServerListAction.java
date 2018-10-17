/*
 * Copyleft (/c) MMXV, Proxy.sh
 * Distributed under the GNU GPL v2
 */
package com.proxysh.shieldtra;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

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

import com.proxysh.shieldtra.service.IPChecker;

import com.proxy.sh.shieldtra.R;
import com.proxysh.shieldtra.service.network.apimodel.ServerResponse;

public class ServerListAction implements OnItemClickListener {

    private ListView listLocation;
    private AppActivity owner;
    private LocationListAdapter listAdapter = null;

    public ServerListAction(View v, AppActivity owner) {

        this.owner = owner;
        listLocation = (ListView) v.findViewById(R.id.listLocation);
        listLocation.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listLocation.setOnItemClickListener(this);
    }

    public void loadLocations() {
        if (listAdapter != null)
            listAdapter = null;

        listAdapter = new LocationListAdapter(this.owner);
        listLocation.setAdapter(listAdapter);
        listLocation.invalidateViews();
    }

    public void refreshLocations() {
        listLocation.invalidateViews();
    }

    public void markActiveLocation(String loc) {

        if (listAdapter == null)
            return;

        if (listAdapter.activeLocation == null || !listAdapter.activeLocation.equals(loc)) {
            clearActiveLocation();
            listAdapter.activeLocation = loc;
            int i = listAdapter.locationIndexes.indexOf(listAdapter.activeLocation);
            listLocation.getAdapter().getView(i + 1,
                    null, listLocation);
        }
    }

    public void clearActiveLocation() {

        if (listAdapter == null)
            return;

        if (listAdapter.activeLocation != null) {
            int i = listAdapter.locationIndexes.indexOf(listAdapter.activeLocation);
            listAdapter.activeLocation = null;
            listLocation.getAdapter().getView(i + 1,
                    null, listLocation);

        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
        // TODO Auto-generated method stub
        listAdapter.setSelectItem(pos);
        ServerResponse selectedServer;
        if (pos == 0) {
            selectedServer = IPChecker.getInstance(this.owner).randomServerForVpn();
        } else {
            ServerResponse sloc = listAdapter.locations.get(pos - 1);
            selectedServer = IPChecker.getInstance(this.owner).serverForVpnByIp(sloc.getIp());
        }
        this.owner.chosenServer = selectedServer;
        this.owner.onSelectServer();
    }


}

class LocationListAdapter extends BaseAdapter {

    public List<ServerResponse> locations;
    public Vector<String> locationIndexes;
    public String activeLocation = null;
    private int itemIndex = -1;
    private Context c;

    public LocationListAdapter(Context c) {
        this.c = c;
        this.locations = IPChecker.getInstance(null).availableServerList();
        this.locationIndexes = new Vector<String>();
        for (int i = 0; i < this.locations.size(); i++) {
            this.locationIndexes.add(this.locations.get(i).getIsoCode().trim());
        }
        Collections.sort(this.locationIndexes, (lhs, rhs) -> {
            int range1 = lhs.lastIndexOf(" ");
            int range2 = rhs.lastIndexOf(" ");
            if (range1 == -1 || range2 == -1) {
                return lhs.compareTo(rhs);
            }

            try {
                int num1 = Integer.valueOf(lhs.substring(range1 + 1));
                int num2 = Integer.valueOf(rhs.substring(range2 + 1));
                if (num1 == 0 || num2 == 0)
                    throw new Exception();

                String pre1 = lhs.substring(0, range1);
                String pre2 = rhs.substring(0, range2);
                int c1 = pre1.compareTo(pre2);
                if (c1 == 0) {
                    if (num1 < num2)
                        return -1;
                    else if (num1 > num2)
                        return 1;

                    return 0;
                }
                return c1;

            } catch (Exception e) {
                return lhs.compareTo(rhs);
            }
        });

    }

    public int getCount() {
        // TODO Auto-generated method stub
        if (locations.isEmpty())
            return 0;
        return locations.size() + 1;
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
            convertView = inflater.inflate(R.layout.location_item_row, null);

            holder = new ViewHolder();
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.textItemLocation = (TextView) convertView.findViewById(R.id.textItemLocation);
        holder.textItemLoad = (TextView) convertView.findViewById(R.id.textItemLoad);
        holder.imageGo = (ImageView) convertView.findViewById(R.id.imageMark);
        if (position == 0) {
            holder.textItemLocation.setText("* Fastest node");
            holder.textItemLoad.setText("(lowest load & ping)");
        } else {
            ServerResponse serverInfo = IPChecker.getInstance(null).serverForVpnByLocation(this.locationIndexes.get(position - 1));
            String location = serverInfo.getIsoCode();
            Integer load = Integer.parseInt(serverInfo.getServerLoad());
//			int imageId = R.drawable.ic_item_go;
//			int colorId = Color.BLACK;
//			if (
//					(activeLocation != null && activeLocation.equals(location)) || 
//					(itemIndex == position)
//			){
//				
//				imageId = R.drawable.ic_item_go_active;
//				colorId = c.getResources().getColor(R.color.ActionTextColor);
//			}
//			holder.imageGo.setImageResource(imageId);
//			holder.textItemLocation.setTextColor(colorId);
//			holder.textItemLoad.setTextColor(colorId);
            holder.textItemLocation.setText(location);
            holder.textItemLoad.setText("(" + load.toString() + "%" + ")");

        }
        return convertView;
    }

    public void setSelectItem(int index) {
        itemIndex = index;
    }

    public int getSelectItem() {
        return itemIndex;
    }


    static class ViewHolder {
        TextView textItemLocation, textItemLoad;
        ImageView imageGo;
    }
}

