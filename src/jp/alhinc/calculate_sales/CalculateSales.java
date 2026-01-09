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

	// 商品定義ファイル名
	private static final String FILE_NAME_COMMODITY_LST = "commodity.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// 商品別集計ファイル名
	private static final String FILE_NAME_COMMODITY_OUT = "commodity.out";

	// 売上ファイル名 正規表現
	private static final String FILE_NAME_REGEX_SALES = "^[0-9]{8}\\.rcd$";

	// 支店コード 正規表現
	private static final String BRANCH_CODE_REGEX = "^[0-9]{3}$";
	// 商品コード 正規表現
	private static final String COMMODITY_CODE_REGEX = "^[0-9a-zA-Z]{8}$";

	// 売上ファイル行数
	private static final int SALES_FILE_LINE_NUM = 3;

	// 売り上げ金額合計上限
	private static final long SALES_AMOUNT_MAX = 10000000000L;

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String BRANCH_DEFINE_FILE = "支店定義ファイル";
	private static final String COMMODITY_DEFINE_FILE = "商品定義ファイル";
	private static final String NOT_EXIST = "%sが存在しません";
	private static final String INVALID_FORMAT = "%sのフォーマットが不正です";
	private static final String SALSE_FILE_NAME_NOT_CONSECUTIVE = "売上ファイル名が連番になっていません";
	private static final String AMOUNT_OVER_DIGITS = "合計金額が10桁を超えました";
	private static final String BRANCH_CODE_INVALID = "%sの支店コードが不正です";
	private static final String COMMODITY_CODE_INVALID = "%sの商品コードが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 商品コードと商品名を保持するMap
		Map<String, String> commodityNames = new HashMap<>();
		// 売上ファイルを保持するList
		ArrayList<File> salesFiles = new ArrayList<File>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();
		// 商品コードと売上金額を保持するMap
		Map<String, Long> commoditySales = new HashMap<>();

		// (エラー処理)コマンドライン引数の確認
		if (args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		String path = args[0];

		// 支店定義ファイル読み込み処理
		if (!readFile(path, FILE_NAME_BRANCH_LST, branchNames, branchSales, BRANCH_CODE_REGEX,
				BRANCH_DEFINE_FILE)) {
			return;
		}

		// 商品定義ファイル読み込み処理
		if (!readFile(path, FILE_NAME_COMMODITY_LST, commodityNames, commoditySales,
				COMMODITY_CODE_REGEX,
				COMMODITY_DEFINE_FILE)) {
			return;
		}

		// 売上ファイルリスト取得処理
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

			// (エラー処理)売り上げファイルが連番か確認
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
			return;
		}

		// 売上ファイルリスト読み込み処理
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

				// (エラー処理)ファイルの中身が規定通りの行数か確認
				if (fileContents.size() != SALES_FILE_LINE_NUM) {
					System.out.println(String.format(INVALID_FORMAT, fileName));
					return;
				}

				String branchCode = fileContents.get(0);
				String commodityCode = fileContents.get(1);
				String fileSales = fileContents.get(2);

				// (エラー処理)支店コード存在有無の確認
				if (!branchNames.containsKey(branchCode)) {
					System.out.println(String.format(BRANCH_CODE_INVALID, fileName));
					return;
				}
				// (エラー処理)商品コード存在有無の確認
				if (!commodityNames.containsKey(commodityCode)) {
					System.out.println(String.format(COMMODITY_CODE_INVALID, fileName));
					return;
				}
				// (エラー処理)売上金額が数値か確認
				if (!fileSales.matches("^[0-9]+$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				// 合計金額の算出
				Long branchSaleAmount = branchSales.get(branchCode) + Long.parseLong(fileSales);
				Long commoditySaleAmount = commoditySales.get(commodityCode) + Long.parseLong(fileSales);

				// (エラー処理)合計金額桁数の確認
				if ((branchSaleAmount >= SALES_AMOUNT_MAX) || (commoditySaleAmount >= SALES_AMOUNT_MAX)) {
					System.out.println(AMOUNT_OVER_DIGITS);
					return;
				}

				// 合計金額の格納
				branchSales.put(branchCode, branchSaleAmount);
				commoditySales.put(commodityCode, commoditySaleAmount);

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
		if (!writeFile(path, FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}
		// 商品別集計ファイル書き込み処理
		if (!writeFile(path, FILE_NAME_COMMODITY_OUT, commodityNames, commoditySales)) {
			return;
		}

	}

	/**
	 * 定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param コードと名前を保持するMap
	 * @param コードと売上金額を保持するMap
	 * @param コードの正規表現
	 * @param ファイルが存在しない場合のエラーメッセージ
	 * @param フォーマットが不正の場合のエラーメッセージ
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> names,
			Map<String, Long> sales, String codeRegex, String errorMessageFileName) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);

			// (エラー処理)定義ファイルの存在有無確認
			if (!file.exists()) {
				System.out.println(String.format(NOT_EXIST, errorMessageFileName));
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while ((line = br.readLine()) != null) {
				// (処理内容)
				final Long INITIAL_SALES = (long) 0;
				String[] items = line.split(",");

				// (エラー処理)定義ファイルのフォーマット確認
				if ((items.length != 2) || (!items[0].matches(codeRegex))) {
					System.out.println(String.format(INVALID_FORMAT, errorMessageFileName));
					return false;
				}

				String code = items[0];
				String name = items[1];

				names.put(code, name);
				sales.put(code, INITIAL_SALES);
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
	 * 集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param コードと名前を保持するMap
	 * @param コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> names,
			Map<String, Long> sales) {
		// 集計ファイル書き込み処理
		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			// 合計金額の書き込み
			for (String key : names.keySet()) {
				bw.write(key + "," + names.get(key) + "," + sales.get(key));
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
