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
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// 売上ファイル名 正規表現
	private static final String FILE_NAME_REGEX_SALES = "^[0-9]{8}\\.rcd$";

	// 支店コード 正規表現
	private static final String BRANCH_CODE_REGEX = "^[0-9]{3}$";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";
	private static final String SALSE_FILE_NAME_NOT_CONSECUTIVE = "売上ファイル名が連番になっていません";
	private static final String AMOUNT_OVER_TEN_DIGITS = "合計金額が10桁を超えました";
	private static final String BRANCH_CODE_NOT_EXIST = "%sの支店コードが不正です";
	private static final String SALES_FILE_CONTENTS_IS_NOT_TWO_LINES = "%sのフォーマットが不正です";

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

		// (エラー処理3-1)コマンドライン引数の確認
		if (args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		String path = args[0];

		// 支店定義ファイル読み込み処理
		if (!readFile(path, FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}

		// (処理内容2-1) 売上ファイルリスト取得処理
		try {
			File[] files = new File(path).listFiles();
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				String fileName = file.getName();
				if (file.isFile() && fileName.matches(FILE_NAME_REGEX_SALES)) {
					salesFiles.add(file);
				}
			}
			Collections.sort(salesFiles);

			// (エラー処理2-1)売り上げファイルが連番か確認
			for (int i = 0; i < salesFiles.size() - 1; i++) {

				int former = Integer.parseInt(salesFiles.get(i).getName().substring(0, 8));
				int latter = Integer.parseInt(salesFiles.get(i + 1).getName().substring(0, 8));

				if ((latter - former) != 1) {
					System.out.println(SALSE_FILE_NAME_NOT_CONSECUTIVE);
					return;
				}
			}
		} catch (Exception e) {
			System.out.println(UNKNOWN_ERROR);
		}

		// (処理内容2-2) 売上ファイルリスト読み込み処理
		for (File salesFile : salesFiles) {
			BufferedReader br = null;
			try {
				FileReader fr = new FileReader(salesFile);
				String fileName = salesFile.getName();
				br = new BufferedReader(fr);
				ArrayList<String> fileContents = new ArrayList<String>();
				String line;

				// 一行ずつ読み込む
				while ((line = br.readLine()) != null) {
					fileContents.add(line);
				}

				// (エラー処理2-4)ファイルの中身が2行か確認
				if (fileContents.size() != 2) {
					System.out
							.println(String.format(SALES_FILE_CONTENTS_IS_NOT_TWO_LINES, fileName));
					return;
				}

				String branchCode = fileContents.get(0);
				String fileSales = fileContents.get(1);

				// (エラー処理2-3)支店コード存在有無の確認
				if (!branchNames.containsKey(branchCode)) {
					System.out.println(String.format(BRANCH_CODE_NOT_EXIST, fileName));
					return;
				}
				// (エラー処理3-2)売上金額が数値か確認
				if (!fileSales.matches("^[0-9]+$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				Long saleAmount = branchSales.get(branchCode) + Long.parseLong(fileSales);

				// (エラー処理2-1)合計金額桁数の確認
				if (saleAmount >= 10000000000L) {
					System.out.println(AMOUNT_OVER_TEN_DIGITS);
					return;
				}
				branchSales.put(branchCode, saleAmount);

			} catch (IOException e) {
				System.out.println(UNKNOWN_ERROR);
			} finally {
				// ファイルを開いている場合
				if (br != null) {
					try {
						// ファイルを閉じる
						br.close();
					} catch (IOException e) {
						System.out.println(UNKNOWN_ERROR);
					}
				}
			}
		}

		// 支店別集計ファイル書き込み処理
		if (!writeFile(path, FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
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

			// (エラー処理1-1)支店定義ファイルの存在有無確認
			if (!file.exists()) {
				System.out.println(FILE_NOT_EXIST);
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while ((line = br.readLine()) != null) {
				// (処理内容1-2)
				final Long INITIAL_BRANCH_SALES = (long) 0;
				String[] items = line.split(",");

				// (エラー処理1-2)支店定義ファイルのフォーマット確認
				if ((items.length != 2) || (!items[0].matches(BRANCH_CODE_REGEX))) {
					System.out.println(FILE_INVALID_FORMAT);
					return false;
				}

				String branchCode = items[0];
				String branchName = items[1];

				branchNames.put(branchCode, branchName);
				branchSales.put(branchCode, INITIAL_BRANCH_SALES);
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
