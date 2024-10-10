package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";
	private static final String SALESFILE_INVALID_NAME = "売上ファイル名が連番になっていません";
	private static final String SPECIFIED_INVALID_FORMAT = /*<ファイル名>*/"のフォーマットが不正です";
	private static final String SPECIFIED_INVALID_BRNCHECODE = /*<ファイル名>*/"の⽀店コードが不正です";
	private static final String SALESAMOUNT_OVER_10DEGITS = "合計金額が10桁を超えました";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {

		//コマンドライン引数の要素数が0（コマンドライン引数なし）のとき、エラーメッセージを表示し、処理を終了
		if (args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		// 支店定義ファイル読み込み処理
		if (!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		//listFilesを使⽤してfilesという配列に、指定したパスに存在する
		//全てのファイル(または、ディレクトリ)の情報を格納
		File[] files = new File(args[0]).listFiles();

		//先にファイルの情報を格納する List(ArrayList) を宣⾔
		List<File> rcdFiles = new ArrayList<>();

		//filesの数だけ繰り返すことで、指定したパスに存在する
		//全てのファイル(または、ディレクトリ)の数だけ繰り返されます。
		for (int i = 0; i < files.length; i++) {

			//files[i].getName() でファイル名が取得できます。
			//ファイル名が「branch.lst」以外であればrcdFilesに追加
			if (files[i].isFile() && files[i].getName().matches("^[0-9]{8}.rcd$")) {
				rcdFiles.add(files[i]);
			}
		}
		//rcd.filesに保持したファイルを昇順にソートする
		Collections.sort(rcdFiles);

		//rcdFilesの売上ファイルのファイル名を１つずつ比較する
		for (int i = 0; i < rcdFiles.size() - 1; i++) {

			//売上ファイル名の0～8文字目を取り出してint型に変換
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));

			//int型に変換した2つの変数を比較して、1にならない場合はエラーメッセージを表示して、処理を終了
			if ((latter - former) != 1) {
				System.out.println(SALESFILE_INVALID_NAME);
				return;
			}
		}

		// 売上ファイルがrcdFilesに複数存在しているので、繰り返しファイルの読み込み行う
		for (int i = 0; i < rcdFiles.size(); i++) {
			BufferedReader br = null;

			try {
				FileReader fr = new FileReader(rcdFiles.get(i));
				br = new BufferedReader(fr);

				String line;
				ArrayList<String> contents = new ArrayList<String>();
				// 一行ずつ読み込む
				while ((line = br.readLine()) != null) {
					//split を使って(改行)で分割すると、
					//contents.get(0) には⽀店コード、contents.get(1) には売上が格納される
					contents.add(line);
				}

				//売上ファイルの中身が3行以上ある場合、エラーメッセージを表示し、処理を終了
				if (contents.size() != 2) {
					System.out.println(rcdFiles.get(i).getName() + SPECIFIED_INVALID_FORMAT);
					return;
				}

				//売上ファイルに記載されている支店コードにが支店定義ファイルに該当しなかった場合、
				//エラーメッセージを表示し、処理を終了
				if (!branchNames.containsKey(contents.get(0))) {
					System.out.println(rcdFiles.get(i).getName() + SPECIFIED_INVALID_BRNCHECODE);
					return;
				}

				//売上ファイルの売上金額に数字以外が記載されている場合、エラーメッセージを表示し、処理を終了
				if (!contents.get(1).matches("^[0-9]*$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				//売上ファイルから読み込んだ売上金額をMapの売上金額と合計する
				Long saleAmount = branchSales.get(contents.get(0)) + Long.parseLong(contents.get(1));

				//売上金額が10桁を超えた場合、エラーメッセージを表示し、処理を終了
				if (saleAmount >= 10000000000L) {
					System.out.println(SALESAMOUNT_OVER_10DEGITS);
					return;
				}

				//合計した売上金額をMapにいれる
				branchSales.put(contents.get(0), saleAmount);

			} catch (IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;
			} finally {
				// ファイルを開いている場合
				if (br != null) {
					try {
						// ファイルを閉じる
						br.close();
					} catch (IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}
		}

		// 支店別集計ファイル書き込み処理
		if (!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}
	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> branchNames,
			Map<String, Long> branchSales) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);

			//支店定義ファイルが格納されていない場合、エラーメッセージを表示して処理終了
			if (!file.exists()) {
				System.out.println(FILE_NOT_EXIST);
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while ((line = br.readLine()) != null) {

				//split を使って「,」(カンマ)で分割すると、
				//items[0] には⽀店コード、items[1] には⽀店名が格納されます。
				String[] items = line.split(",");

				//ファイル内が下記の２条件に合わない場合は、エラーメッセージを表示して処理終了
				if ((items.length != 2) || (!items[0].matches("^[0-9]{3}$"))) {
					System.out.println(FILE_INVALID_FORMAT);
					return false;
				}

				//Mapに追加する2つの情報をputの引数として指定します。
				//branchNamesのMapに支店定義ファイルの支店コード、支店名をいれる
				branchNames.put(items[0], items[1]);
				//branchSalesのMapに支店コードと0（売上ファイル取り込み前のため）をLong型でいれる
				branchSales.put(items[0], 0L);
			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if (br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> branchNames,
			Map<String, Long> branchSales) {
		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			for (String key : branchNames.keySet()) {

				//支店別集計ファイルに出力
				bw.write(key + "," + branchNames.get(key) + "," + branchSales.get(key).toString());

				//改行
				bw.newLine();
			}
		} catch (IOException e) {
			System.out.println(e);
			return false;
		} finally {
			// ファイルを開いている場合
			if (bw != null) {
				try {
					// ファイルを閉じる
					bw.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}

		return true;
	}

}
