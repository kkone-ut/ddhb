package jp.ac.shibaura_it.ma15082;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

public enum Tools {
	instance;
	private Random rand;

	private Tools() {
		rand = new Random();
	}

	public static int rand(int max) {
		return instance.rand.nextInt(max);
	}

	public static double random() {
		return Math.random();
	}

	public static double random(double max) {
		return random() * max;
	}

	public static double random(double min, double max) {
		return min + random(max - min);
	}

	public static double random(Pair<Double, Double> d) {
		if (d == null || d.getKey() == null || d.getValue() == null) {
			return random();
		}

		return random(d.getKey(), d.getValue());
	}

	public static double meadian(List<Double> list) {
		List<Double> temp = new ArrayList<Double>(list);
		Collections.sort(temp);

		int size = temp.size();
		if (size <= 0) {
			return 0;
		}
		int index = size / 2;
		if (size % 2 == 0) {
			return (temp.get(index) + temp.get(index - 1)) / 2.0;
		}
		return temp.get(index);

	}

	private static <T> void ssort(ListMap<T, Double> target, boolean flag) {
		List<T> keys = target.keyList();
		List<Double> values = target.valueList();
		int h = 1;
		while (h * 9 < target.size()) {
			h = 3 * h + 1;
		}
		if (flag) {
			while (h > 0) {
				for (int i = h; i < target.size(); ++i) {

					double temp = values.get(i);
					double cur = values.get(i - h);
					if (cur > temp) {
						int j = i;
						T key = keys.get(i);
						do {
							values.set(j, cur);
							keys.set(j, keys.get(j - h));
							j -= h;
						} while (j >= h && (cur = values.get(j - h)) > temp);
						values.set(j, temp);
						keys.set(j, key);
					}
				}
				h /= 3;
			}

		} else {
			while (h > 0) {
				for (int i = h; i < target.size(); ++i) {
					double temp = values.get(i);
					double cur = values.get(i - h);
					if (cur < temp) {
						int j = i;
						T key = keys.get(i);
						do {
							values.set(j, cur);
							keys.set(j, keys.get(j - h));
							j -= h;
						} while (j >= h && (cur = values.get(j - h)) < temp);
						values.set(j, temp);
						keys.set(j, key);
					}
				}
				h /= 3;
			}
		}

		for (int i = 0; i < target.size(); i++) {
			target.setKey(i, keys.get(i));
			target.setValue(i, values.get(i));
		}

		return;
	}

	public static <T> void sort(ListMap<T, Double> target, boolean flag) {
		ssort(target, flag);
		return;

	}

	public static <T> List<T> limit(List<T> target, int limit) {
		int t;
		while (target.size() > limit) {
			t = Tools.rand(target.size());
			target.remove(t);
		}

		return target;

	}

	public static <K> K selectMaxKey(ListMap<K, Double> ss) {
		K ret = null;
		double max = 0;
		for (int i = 0; i < ss.size(); ++i) {
			K key = ss.getKey(i);
			double value = ss.getValue(i);
			if (max < value) {
				max = value;
				ret = key;
			}
		}
		return ret;
	}

	public static <K> K selectKey(ListMap<K, Double> ss) {
		K ret = null;
		double sum = 0;
		// sort(ss, true);
		// System.out.println(ss);
		for (Double s : ss.valueList()) {
			sum += s;
		}
		if (sum < 0.0) {
			return ret;
		}
		double level = random(sum);
		for (int i = 0; i < ss.size(); ++i) {
			double temp = ss.getValue(i);
			if (temp <= 0.0) {
				continue;
			}
			level -= temp;
			if (level <= 0.0) {
				ret = ss.getKey(i);
				break;
			}
		}
		return ret;
	}

	public static <K> List<K> sortKeyList(ListMap<K, Double> list) {
		List<K> ret = new ArrayList<K>();
		List<Entry<K, Double>> temp = list.entryList();
		Collections.sort(temp, new Comparator<Entry<K, Double>>() {
			@Override
			public int compare(Entry<K, Double> lhs, Entry<K, Double> rhs) {
				if (lhs.getValue() < rhs.getValue()) {
					return 1;
				}
				if (lhs.getValue() > rhs.getValue()) {
					return -1;
				}
				return 0;
			}
		});
		// System.out.println(temp);
		for (Entry<K, Double> entry : temp) {
			if (entry.getValue() > 0.0) {
				ret.add(entry.getKey());
			}
		}

		return ret;
	}

	private static double ward(List<Double> level, int from, int mid, int to) {
		double score = 0;
		double m1 = 0, m2 = 0, m = 0;
		for (int i = from; i < mid; ++i) {
			m1 += level.get(i);
		}
		for (int i = mid; i < to; ++i) {
			m2 += level.get(i);
		}
		m = (m1 + m2) / (to - from);
		m1 /= (mid - from);
		m2 /= (to - mid);

		for (int i = from; i < to; ++i) {
			double temp1 = level.get(i) - m;
			double temp2 = level.get(i);
			if (i < mid) {
				temp2 -= m1;
			} else {
				temp2 -= m2;
			}

			score += square(temp1) - square(temp2);

		}

		return score;
	}

	private static double ward2(double sum1, double sum2, int from, int mid, int to) {
		// int length1=(mid)-(from);
		// int length2=(to)-(mid);
		// int length=length1+length2;

		// double sum=sum1+sum2;
		// double avr1=sum1/length1;
		// double avr2=sum2/length2;
		// double avr=sum/length;

		double ret = square(sum1) / (mid - from) + square(sum2) / (to - mid) - square(sum1 + sum2) / (to - from);
		return ret > 0 ? ret : 0;
	}

	public static ListMap<Integer, Double> clustering(List<Double> level) {
		Collections.sort(level);
		ListMap<Integer, Double> ret = new ListMap<Integer, Double>(level.size());
		List<Integer> rem = new ArrayList<Integer>(level.size() + 1);
		int zero = 0;
		while (zero < level.size() && level.get(zero) <= 0.0) {
			zero++;
		}
		if (zero >= level.size()) {
			return null;
		}
		if (zero != 0) {
			rem.add(0);
		}
		for (int i = zero; i <= level.size(); ++i) {
			rem.add(i);
		}
		for (int i = 0; i < zero; ++i) {
			ret.put(i, 0.0);
		}

		// List<Double> sumlist=new ArrayList<Double>();
		double[] sumlist = new double[rem.size() - 1];
		// double[] dif=new double[rem.size()-2];
		List<Double> dif = new ArrayList<Double>(rem.size() - 2);

		double prev = 0;
		for (int j = rem.get(0); j < rem.get(1); j++) {
			prev += level.get(j);
		}
		// sumlist.add(prev);
		sumlist[0] = prev;

		for (int i = 1; i < rem.size() - 1; ++i) {
			double sum = 0;
			for (int j = rem.get(i); j < rem.get(i + 1); j++) {
				sum += level.get(j);
			}
			// sumlist.add(sum);
			sumlist[i] = sum;
			dif.add(ward2(prev, sum, rem.get(i - 1), rem.get(i), rem.get(i + 1)));
			prev = sum;
		}

		while ((dif.size()) > 0) {
			int index = 0;

			double min = dif.get(0);
			for (int i = 1; i < dif.size(); ++i) {
				if (dif.get(i) < min) {
					index = i;
					min = dif.get(i);
				}
			}
			double sum = sumlist[index] + sumlist[index + 1];
			if (0 < (index) && (index) < (rem.size() - 2)) {
				dif.set(index - 1,
						ward2(sumlist[(index - 1)], sum, rem.get(index - 1), rem.get(index), rem.get(index + 2)));
			}
			if (0 <= (index) && (index) < (rem.size() - 3)) {
				dif.set(index + 1,
						ward2(sum, sumlist[(index + 2)], rem.get(index), rem.get(index + 2), rem.get(index + 3)));
			}
			sumlist[index] = sum;

			int size = rem.size() - 1;
			for (int i = index + 2; i < size; ++i) {
				sumlist[i - 1] = sumlist[i];
			}

			Integer key = rem.remove(index + 1);
			Double value = dif.remove(index);
			ret.put(key, value);

		}

		return ret;
	}

	public static <T> void unit(ListMap<T, Double> v) {
		double sum = 0;
		for (double s : v.valueList()) {
			sum += s;
		}
		if (sum == 0.0) {
			if (v.size() == 1) {
				v.setValue(0, 1.0);
			}
			return;
		}
		for (T k : v.keySet()) {
			v.put(k, v.get(k) / sum);
		}
		return;
	}

	public static <T> void cut(ListMap<T, Double> v, int num) {
		sort(v, false);
		for (int i = v.size() - 1; i >= num; --i) {
			v.setValue(i, 0.0);
		}

		return;
	}

	public static <T> boolean cutList(ListMap<T, Double> list, int cluster, int num) {
		int size = calcLimitSize(list.valueList(), cluster, num);
		// System.out.println(size+" "+list);
		if (size < 0) {
			return false;
		}
		Tools.cut(list, size);
		return true;
	}

	public static int calcLimitSize(List<Double> level, int cluster, int num) {
		ListMap<Integer, Double> ans = clustering(level);
		if (ans == null) {
			return -1;
		}
		List<Integer> temp = ans.keyList();
		List<Double> val = ans.valueList();

		int c_max = 1;
		for (int i = 0; i < val.size(); ++i) {
			if (val.get(i) > 0.0) {
				c_max++;
			}
		}
		if (c_max < cluster) {
			cluster = c_max;
		}
		if (cluster <= num) {
			num = c_max - 1;
		}
		int[] max = new int[cluster];
		for (int i = 1; i < cluster; ++i) {
			max[i] = temp.get(temp.size() - i);
		}
		Arrays.sort(max, 1, cluster);
		return (level.size() - max[cluster - num]);

	}

	public static double root(double t) {
		return Math.sqrt(t);
	}

	public static double square(double t) {
		return t * t;
	}

}
