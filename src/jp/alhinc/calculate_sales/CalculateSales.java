package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// 売上ファイル名 正規表現
	private static final String FILE_NAME_REGEX_SALES = "^[0-9]{8}\\.rcd$";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();
		// 売上ファイルを保持するList
		ArrayList<File> salesFiles = new ArrayList<File>();

		// 支店定義ファイル読み込み処理
		if (!readBranchDifinitionFile(args[0], FILE_NAME_BRANCH_LST, branchNames)) {
			return;
		}

		// (処理内容2-1) 売上ファイルリスト取得処理
		if (!getSalesFiles(args[0], salesFiles)) {
			return;
		}

		// (処理内容2-2) 売上ファイルリスト読み込み処理
		for (File salesFile : salesFiles) {
			if (!readSalesFile(salesFile, branchNames, branchSales)) {
				return;
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
	 * @return 読み込み可否
	 */
	private static boolean readBranchDifinitionFile(String path, String fileName, Map<String, String> branchNames) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while ((line = br.readLine()) != null) {
				// (処理内容1-2)
				String[] items = line.split(",");
				String branchCode = items[0];
				String branchName = items[1];

				branchNames.put(branchCode, branchName);
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
	 * 売上ファイルリスト取得処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 売り上げファイルを保持するList
	 * @return 読み込み可否
	 */
	private static boolean getSalesFiles(String path, ArrayList<File> salesFiles) {

		try {
			File[] files = new File(path).listFiles();
			for (File file : files) {
				String fileName = file.getName();
				if (fileName.matches(FILE_NAME_REGEX_SALES)) {
					salesFiles.add(file);
				}
			}
		} catch (Exception e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		}

		return true;
	}

	/**
	 * 売上ファイル読み込み処理
	 *
	 * @param 売り上げファイルを保持するList
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readSalesFile(File salesFile, Map<String, String> branchNames,
			Map<String, Long> branchSales) {
		BufferedReader br = null;

		try {
			FileReader fr = new FileReader(salesFile);
			br = new BufferedReader(fr);
			ArrayList<String> fileContents = new ArrayList<String>();
			String line;

			// 一行ずつ読み込む
			while ((line = br.readLine()) != null) {
				fileContents.add(line);
			}

			String branchCode = fileContents.get(0);
			Long fileSale = Long.parseLong(fileContents.get(1));

			// 支店定義ファイルにない支店コードなら合計処理をしない
			if (!branchNames.containsKey(branchCode)) {
				return false;
			}

			// 暫定の合計金額を算出
			Long saleAmount = (long) 0;
			if (branchSales.containsKey(branchCode)) {
				saleAmount = branchSales.get(branchCode);
			}
			saleAmount += fileSale;

			// 売り上げの合計金額を更新
			branchSales.put(branchCode, saleAmount);

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
		// (処理内容3-1)
		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			// 合計金額の書き込み
			for (String key : branchNames.keySet()) {
				bw.write(key + "," + branchNames.get(key) + "," + branchSales.get(key));
				bw.newLine();
			}
		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			if (bw != null) {
				try {
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
