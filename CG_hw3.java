import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;


public class CG_hw3
{

	int TOP = 0;
	int BOTTOM = 1;
	int LEFT = 2;
	int RIGHT = 3;
	int clip_boundary;
	float intersect_x, intersect_y;
	int world_x1 = 0, world_y1 = 0, world_x2 = 250, world_y2 = 250;
	int view_x1 = 0, view_y1 = 0, view_x2 = 200, view_y2 = 200;
	float scaling_factor = 1.0f;
	int width;
	int height;
	int rotation = 0, translation_x = 0, translation_y = 0;
	int pixels [][];
	int temp;
	String input = "hw3_split.ps";
	List<List<List<Integer>>> all_polygons = new ArrayList<List<List<Integer>>>();
	List<List<List<Float>>> transformed_lines = new ArrayList<List<List<Float>>>();
	List<List<List<Float>>> clipped_lines = new ArrayList<List<List<Float>>>();
	List<List<Float>> polygon = new ArrayList<List<Float>>();
	List<List<List<Float>>> viewport_polygons = new ArrayList<List<List<Float>>>();
	List<List<List<Integer>>> final_polygons = new ArrayList<List<List<Integer>>>();
	List<List<List<Integer>>> intersection_points = new ArrayList<List<List<Integer>>>();
	List<List<List<Integer>>> edge_list = new ArrayList<List<List<Integer>>>();


	public int getMax(List<List<Integer>> list)
	{
		int max = Integer.MIN_VALUE;
		for(int i=0; i<list.size(); i++)
		{
			int y = list.get(i).get(1);

			if(y > max)
				max = y;
		}
		return max;
	}

	public int getMin(List<List<Integer>> list)
	{
		int min = Integer.MAX_VALUE;
		for(int i=0; i<list.size(); i++)
		{
			int y = list.get(i).get(1);

			if(y < min)
				min = y;
		}
		return min;
	}

	public void scan_fill()
	{
		for(int i=0; i<intersection_points.size(); i++)
		{
			for(int j=0; j<intersection_points.get(i).size(); j+=2)
			{

				int x1 = intersection_points.get(i).get(j).get(0);
				int y1 = intersection_points.get(i).get(j).get(1);
				int x2 = intersection_points.get(i).get(j+1).get(0);
				//int y2 = intersection_points.get(i).get(j+1).get(1);

				while(x1 != x2)
				{
					pixels[y1][x1] = 1;

					x1++;
				}
			}
		}

	}
	public void sorting()
	{
		//Sorting
		for(int i=0; i<intersection_points.size(); i++)
		{
			Collections.sort(intersection_points.get(i), new Comparator<List<Integer>>() 
					{
				public int compare(List<Integer> o1, List<Integer> o2)
				{
					return o1.get(0).compareTo(o2.get(0));
				}
					});
		}
	}

	public void calculate_intersection(int x1, int y1, int x2, int y2, int y, int ymin)
	{
		List<Integer> row = new ArrayList<Integer>();

		float dx = x2 - x1;
		float dy = y2 - y1;

		int x = Math.round(x1 + (dx/dy)*(y - y1));

		row.add(x);
		row.add(y);

		intersection_points.get(y-ymin).add(row);
	}

	public void polygon_filling()
	{
		//Scanline filling
		for(int i=0; i<final_polygons.size(); i++)
		{
			int ymax = getMax(final_polygons.get(i));
			int ymin = getMin(final_polygons.get(i));

			for(int s=ymin; s<=ymax; s++)
			{
				edge_list.add(new ArrayList<List<Integer>>());
				intersection_points.add(new ArrayList<List<Integer>>());
			}

			for(int j=0; j<final_polygons.get(i).size() - 1; j++)  
			{

				int ymin_edge = final_polygons.get(i).get(j).get(1);
				int ymax_edge = final_polygons.get(i).get(j+1).get(1);

				for(int y = ymin; y<=ymax; y++)    //For every scanline
				{

					if(((y >= ymin_edge && y < ymax_edge) || (y >= ymax_edge && y < ymin_edge)) && ymin_edge != ymax_edge)
					{
						List<Integer> row = new ArrayList<Integer>();
						int x1 = final_polygons.get(i).get(j).get(0);
						int x2 = final_polygons.get(i).get(j+1).get(0);

						row.add(x1);
						row.add(ymin_edge);
						row.add(x2);
						row.add(ymax_edge);

						edge_list.get(y-ymin).add(row);
					}
				}  
			}

			for(int y = ymin; y <= ymax; y++)
			{
				intersection_points.clear();
				for(int s=ymin; s<=ymax; s++)
				{
					intersection_points.add(new ArrayList<List<Integer>>());
				}

				for(int j = 0; j<edge_list.get(y-ymin).size(); j++)
				{
					int x1 = edge_list.get(y-ymin).get(j).get(0);
					int y1 = edge_list.get(y-ymin).get(j).get(1);
					int x2 = edge_list.get(y-ymin).get(j).get(2);
					int y2 = edge_list.get(y-ymin).get(j).get(3);

					calculate_intersection(x1, y1, x2, y2, y, ymin);
				}

				sorting();
				scan_fill();
			}

			edge_list.clear();
			intersection_points.clear();
		}
	}

	public void round_int()
	{
		for(int i=0; i<viewport_polygons.size(); i++)
		{
			List<List<Integer>> polygon = new ArrayList<List<Integer>>();

			for(int j=0; j<viewport_polygons.get(i).size(); j++)
			{
				List<Integer> row = new ArrayList<Integer>();

				int x = Math.round(viewport_polygons.get(i).get(j).get(0));
				int y = Math.round(viewport_polygons.get(i).get(j).get(1));

				row.add(x);
				row.add(y);
				polygon.add(row);
			}

			final_polygons.add(polygon);
		}
	}
	public void viewport_transformation()
	{
		//Translation to origin of world window
		List<List<List<Float>>> translated_polygons = new ArrayList<List<List<Float>>>();

		for(int i=0; i<clipped_lines.size(); i++)
		{
			List<List<Float>> polygon = new ArrayList<List<Float>>();

			for(int j=0; j<clipped_lines.get(i).size(); j++)
			{

				List<Float> row = new ArrayList<Float>();

				float x = clipped_lines.get(i).get(j).get(0);
				float y = clipped_lines.get(i).get(j).get(1);
				x = x - world_x1;
				y = y - world_y1;
				row.add(x);
				row.add(y);
				polygon.add(row);
			}

			translated_polygons.add(polygon);
		}

		//Scaling to viewport
		List<List<List<Float>>> scaled_polygons = new ArrayList<List<List<Float>>>();

		for (int i=0; i<translated_polygons.size(); i++)
		{
			List<List<Float>> polygon = new ArrayList<List<Float>>();

			for(int j=0; j<translated_polygons.get(i).size(); j++)
			{
				List<Float> row = new ArrayList<Float>();

				float x = translated_polygons.get(i).get(j).get(0);
				float y = translated_polygons.get(i).get(j).get(1);
				float num_x = view_x2-view_x1;
				float num_y = view_y2-view_y1;
				float den_x = world_x2-world_x1;
				float den_y = world_y2-world_y1;
				x = x * (num_x/den_x);
				y = y * (num_y/den_y);
				row.add(x);
				row.add(y);
				polygon.add(row);	
			}
			scaled_polygons.add(polygon);
		}

		//Translating to viewport origin
		for(int i=0; i<scaled_polygons.size(); i++)
		{
			List<List<Float>> polygon = new ArrayList<List<Float>>();

			for(int j=0; j<scaled_polygons.get(i).size(); j++)
			{

				List<Float> row = new ArrayList<Float>();

				float x = scaled_polygons.get(i).get(j).get(0);
				float y = scaled_polygons.get(i).get(j).get(1);
				x = x + view_x1;
				y = y + view_y1;
				row.add(x);
				row.add(y);
				polygon.add(row);
			}

			viewport_polygons.add(polygon);

		}

	}
	public void transfer_points()
	{
		transformed_lines.clear();

		for(int i = 0; i < clipped_lines.size(); i++)
		{
			List<List<Float>> polygon = new ArrayList<List<Float>>();

			for(int j=0; j<clipped_lines.get(i).size(); j++)
			{
				List<Float> row = new ArrayList<Float>();

				float x = clipped_lines.get(i).get(j).get(0);
				float y = clipped_lines.get(i).get(j).get(1);

				row.add(x);
				row.add(y);
				polygon.add(row);
			}
			transformed_lines.add(polygon);
		}

		clipped_lines.clear();

	}

	public void clipping()
	{
		//Sutherland-Hodgman clipping

		clip_boundary = TOP;
		clipper();
		transfer_points();

		clip_boundary = RIGHT;
		clipper();
		transfer_points();

		clip_boundary = BOTTOM;
		clipper();
		transfer_points();

		clip_boundary = LEFT;
		clipper();
	}

	public void output_vertex(float x, float y)
	{
		List<Float> row = new ArrayList<Float>();
		row.add(x);
		row.add(y);

		polygon.add(row);

	}

	public void clipper()
	{
		for(int i=0; i<transformed_lines.size(); i++)
		{
			for(int j=0; j<transformed_lines.get(i).size()-1; j++)
			{
				float x1 = transformed_lines.get(i).get(j).get(0);
				float y1 = transformed_lines.get(i).get(j).get(1);
				float x2 = transformed_lines.get(i).get(j+1).get(0);
				float y2 = transformed_lines.get(i).get(j+1).get(1);

				if(inside(x1, y1))
				{	
					if(j == 0)
						output_vertex(x1, y1);

					if(inside(x2, y2))
					{
						output_vertex(x2, y2);

					}
					else
					{
						intersection(x1, y1, x2, y2, clip_boundary);
						output_vertex(intersect_x, intersect_y);
					}
				}
				else
				{
					if(inside(x2, y2))
					{
						intersection(x2, y2, x1, y1, clip_boundary);
						output_vertex(intersect_x, intersect_y);

						output_vertex(x2, y2);
					}
				}
			}
			if(!polygon.isEmpty())
				clipped_lines.add(polygon);
			polygon = new ArrayList<List<Float>>();
		}

		//Add the first point to the last for all polygons
		for(int i=0; i<clipped_lines.size(); i++)
		{			
			List<Float> row = new ArrayList<Float>();

			float x =clipped_lines.get(i).get(0).get(0);
			float y =clipped_lines.get(i).get(0).get(1);

			row.add(x);
			row.add(y);

			clipped_lines.get(i).add(row);	
		}
	}

	public void intersection(float x1, float y1, float x2, float y2, int clip_boundary)
	{
		float dx = x2 - x1;
		float dy = y2 - y1;

		float slope = dy/dx;

		//Vertical line condition
		if(dx == 0 || dy == 0)
		{

			if(clip_boundary == TOP)
			{
				intersect_x = x1;
				intersect_y = world_y2;
			}
			else if(clip_boundary == LEFT)
			{
				intersect_x = world_x1;
				intersect_y = y1;
			}
			else if(clip_boundary == BOTTOM)
			{
				intersect_x = x1;
				intersect_y = world_y1;
			}
			else if(clip_boundary == RIGHT)
			{
				intersect_x = world_x2;
				intersect_y = y1;
			}

			return;
		}

		if(clip_boundary == LEFT)
		{
			intersect_x = world_x1;
			intersect_y = slope * (world_x1 - x1) + y1;
		}
		if(clip_boundary == RIGHT)
		{
			intersect_x = world_x2;
			intersect_y = slope * (world_x2 - x1) + y1;
		}
		if(clip_boundary == TOP)
		{
			intersect_x = (world_y2 - y1)/slope + x1;
			intersect_y = world_y2;
		}
		if(clip_boundary == BOTTOM)
		{
			intersect_x = (world_y1 - y1)/slope + x2;
			intersect_y = world_y1;
		}
	}

	public boolean inside(float x, float y)
	{

		if(clip_boundary == TOP && y < world_y2)
			return true;

		else if(clip_boundary == LEFT && x > world_x1)
			return true;

		else if(clip_boundary == BOTTOM && y > world_y1)
			return true;

		else if(clip_boundary == RIGHT && x < world_x2)
			return true;

		return false;

	}

	/*
	public void drawing()
	{
		for (int i=0; i<height; i++)
		{
			for (int j=0; j<width; j++)
			{
				pixels[i][j] = 0;
			}	
		}

		for (int i=0; i<intersection_points.size(); i+=2)
		{

			float x1 = intersection_points.get(i).get(0);
			float y1 = intersection_points.get(i).get(1);
			float x2 = intersection_points.get(i+1).get(0);
			float y2 = intersection_points.get(i+1).get(1);


			//DDA
			float steps;
			float xc,yc;
			float x,y;

			float dx = x2 - x1;
			float dy = y2 - y1;

			if(Math.abs(dx) > Math.abs(dy))
				steps = Math.abs(dx);

			else
				steps = Math.abs(dy);

			if(x1 == x2 && dy < 0)
				steps =  Math.abs(dy);

			xc = dx/steps;

			yc = dy/steps;

			x = x1;

			y = y1;

			for (int s=0; s<steps; s++)
			{
				//if(!(x < view_x1 || y < view_y1 || x > view_x2 || y > view_y2))
				pixels[Math.round(y)][Math.round(x)] = 1;

				x = x + xc;
				y = y + yc;
			}
		}
	}
	 */
	public void output() throws FileNotFoundException, UnsupportedEncodingException
	{
		System.out.println("/*XPM*/");
		System.out.println("static char *sco100[] = { ");
		System.out.println("/* width height num_colors chars_per_pixel */ ");
		System.out.println("\""+ width + " " + height + " " + "2" + " " + "1" + "\"" + ",");
		System.out.println("/*colors*/");
		System.out.println("\""+ "0" + " " + "c" + " " + "#" + "ffffff" + "\"" + "," );
		System.out.println("\""+ "1" + " " + "c" + " " + "#" + "000000" + "\"" + "," );
		System.out.println("/*pixels*/");
		for (int i=0; i<height; i++)
		{
			System.out.print("\"");
			for(int j=0; j<width; j++)
			{
				System.out.print(pixels[height-i-1][j]);
			}
			if(i == height - 1)
				System.out.print("\"");
			else
				System.out.print("\"" + ",");

			System.out.println();
		}

		System.out.println("};");
	}

	public void transformation()
	{
		//Scaling

		List<List<List<Float>>> scaled_lines = new ArrayList<List<List<Float>>>();

		for (int i=0; i<all_polygons.size(); i++)
		{
			List<List<Float>> polygon = new ArrayList<List<Float>>();

			for(int j=0; j<all_polygons.get(i).size(); j++)
			{
				List<Float> row = new ArrayList<Float>();

				for(int k=0; k<2; k++)
				{
					float temp = all_polygons.get(i).get(j).get(k);
					temp = temp * scaling_factor;
					row.add(temp);
				}
				polygon.add(row);	
			}
			scaled_lines.add(polygon);
		}

		//Rotation
		List<List<List<Float>>> rotated_lines = new ArrayList<List<List<Float>>>();

		for(int i=0; i<scaled_lines.size(); i++)
		{
			List<List<Float>> polygon = new ArrayList<List<Float>>();

			for(int j=0; j<all_polygons.get(i).size(); j++)
			{
				List<Float> row = new ArrayList<Float>();

				float x = scaled_lines.get(i).get(j).get(0);
				float y = scaled_lines.get(i).get(j).get(1);
				double x_prime = x * Math.cos(Math.toRadians(rotation)) - y * Math.sin(Math.toRadians(rotation));
				double y_prime = x * Math.sin(Math.toRadians(rotation)) + y * Math.cos(Math.toRadians(rotation));

				row.add((float)x_prime);
				row.add((float)y_prime);
				polygon.add(row);
			}
			rotated_lines.add(polygon);
		}

		//Translation
		for(int i=0; i<rotated_lines.size(); i++)
		{
			List<List<Float>> polygon = new ArrayList<List<Float>>();

			for(int j=0; j<all_polygons.get(i).size(); j++)
			{

				List<Float> row = new ArrayList<Float>();

				float x = rotated_lines.get(i).get(j).get(0);
				float y = rotated_lines.get(i).get(j).get(1);
				x = x + translation_x;
				y = y + translation_y;
				row.add(x);
				row.add(y);
				polygon.add(row);
			}

			transformed_lines.add(polygon);
		}

	}

	public void read_file(String input) throws FileNotFoundException
	{
		File file = new File(input);
		Scanner sc = new Scanner(file);
		while(sc.hasNextLine())
		{
			if(sc.nextLine().equals("%%%BEGIN"))
			{
				List<List<Integer>> polygon = new ArrayList<List<Integer>>();
				String line = sc.nextLine();

				while(sc.hasNextLine())
				{
					line = sc.nextLine();

					if(line.equals("%%%END"))
						break;

					if(line.trim().length() == 0)               //For blank lines
						continue;

					if(line.equals("stroke"))                     //One polygon done
					{
						all_polygons.add(polygon);
						polygon = new ArrayList<List<Integer>>();
						continue;
					}

					String parse[] = line.split(" ");

					int x,y;

					if(parse[0].equals(""))
					{
						x = Integer.parseInt(parse[1]);
						if(parse[2].equals(""))
							y = Integer.parseInt(parse[3]);
						else
							y = Integer.parseInt(parse[2]);
					}

					else
					{
						x = Integer.parseInt(parse[0]);
						if(parse[1].equals(""))
							y = Integer.parseInt(parse[2]);
						else
							y = Integer.parseInt(parse[1]);
					}

					List<Integer> row = new ArrayList<Integer>();
					row.add(x);
					row.add(y);
					polygon.add(row);
				}
			}				

		}

		sc.close();   
	}

	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException
	{
		CG_hw3 obj = new CG_hw3();

		for (int i=0; i<args.length; i+=2)
		{
			if(args[i].equals("-f"))
				obj.input = args[i+1];

			if(args[i].equals("-a"))
				obj.world_x1 = Integer.parseInt(args[i+1]);

			if(args[i].equals("-b"))
				obj.world_y1 = Integer.parseInt(args[i+1]);

			if(args[i].equals("-c"))
				obj.world_x2 = Integer.parseInt(args[i+1]);

			if(args[i].equals("-d"))
				obj.world_y2 = Integer.parseInt(args[i+1]);

			if(args[i].equals("-r"))
				obj.rotation = Integer.parseInt(args[i+1]);

			if(args[i].equals("-m"))
				obj.translation_x = Integer.parseInt(args[i+1]);

			if(args[i].equals("-n"))
				obj.translation_y = Integer.parseInt(args[i+1]);

			if(args[i].equals("-s"))
				obj.scaling_factor = Float.parseFloat(args[i+1]);

			if(args[i].equals("-j"))
				obj.view_x1 = Integer.parseInt(args[i+1]);

			if(args[i].equals("-k"))
				obj.view_y1 = Integer.parseInt(args[i+1]);

			if(args[i].equals("-o"))
				obj.view_x2 = Integer.parseInt(args[i+1]);

			if(args[i].equals("-p"))
				obj.view_y2 = Integer.parseInt(args[i+1]);

		}

		obj.read_file(obj.input);
		obj.width = 501;
		obj.height = 501;
		obj.pixels = new int[obj.height][obj.width];
		obj.transformation();
		obj.clipping();
		obj.viewport_transformation();
		obj.round_int();
		obj.polygon_filling();        
		//obj.drawing();
		obj.output();

		/*
		for(int i=0; i<obj.intersection_points.size(); i++)
		{
			for(int j=0; j<obj.intersection_points.get(i).size(); j++)
			{
				for (int k=0; k<2; k++)
				{
					System.out.print(obj.intersection_points.get(i).get(j).get(k) + " ");
				}
				System.out.println();
			}
			System.out.println("----------");
		}

		/*
		for(int i=0; i< obj.final_polygons.size(); i++)
		{
			for(int j=0; j<obj.final_polygons.get(i).size(); j++)
			{
				for(int k=0; k<2; k++)
				{
					System.out.print(obj.final_polygons.get(i).get(j).get(k) + " ");
				}
				System.out.println();
			}
			System.out.println("------------");
		}

		 */

	}

}
