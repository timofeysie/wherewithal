package com.curchod.domartin;

import android.R;
import android.content.Context;
import android.widget.ArrayAdapter;

public class CustomAdapter extends ArrayAdapter<String> 
{
	  private final Context context;
	  private final String[] values;

	  public CustomAdapter(Context context, String[] values) 
	  {
		    super(context, R.layout.simple_list_item_1, values);
		    this.context = context;
		    this.values = values;
		  }

	  /*
		  @Override
		  public View getView(int position, View convertView, ViewGroup parent) 
		  {
		    LayoutInflater inflater = (LayoutInflater) context
		        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		    View rowView = inflater.inflate(R.layout.simple_list_item_1, parent, false);
		    TextView textView = (TextView) rowView.findViewById(R.id.text1);
		    ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
		    textView.setText(values[position]);
		    // Change the icon for Windows and iPhone
		    if(rowView == null)
            {
                if((position % 2)==1){ 
                rowView.setBackgroundColor(Color.BLACK)
                    //colorList :setBackgroundColor( colorList.get(position) )
                }
                 else{
                    rowView.setBackgroundColor(Color.RED)
                }
             }           
            return rowView;

		    return rowView;
		  }
		  */
}
