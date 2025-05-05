import pandas as pd


def read_multiple_csv(*csv_files):

    dfs = []
    for file_path in csv_files:
        try:
            df = pd.read_csv(file_path)
            dfs.append(df)
        except Exception as e:
            print(f"Произошла ошибка при чтении файла {file_path}: {e}")
    return dfs


def calculate_coal_balance(row, supplies):
    # Берём ключевые поля из текущей строки
    current_date = row['ВыгрузкаНаСклад']
    warehouse = row['Склад']
    pile = row['Штабель']
    cargo_type = row['Марка']

    # Фильтруем запасы угля на складе до текущей даты
    incoming = supplies[
        (supplies['ВыгрузкаНаСклад'] <= current_date)
        & (supplies['Склад'] == warehouse)
        & (supplies['Штабель'] == pile)
        & (supplies['Марка'] == cargo_type)
        ]['На склад, тн'].sum()

    # Фильтруем объёмы угля, отправленные с судов до текущей даты
    outgoing = supplies[
        (supplies['ПогрузкаНаСудно'] <= current_date)
        & (supplies['Склад'] == warehouse)
        & (supplies['Штабель'] == pile)
        & (supplies['Марка'] == cargo_type)
        ]['На судно, тн'].sum()

    # Баланс равен количеству принятого угля минус отправленному
    balance = incoming - outgoing
    return balance


# def prepare_and_merge_data():
#     dfs = read_multiple_csv('supplies.csv', "fires.csv", 'temperature.csv', 'weather_data_2015.csv', 'weather_data_2016.csv',
#                             'weather_data_2017.csv', 'weather_data_2018.csv', 'weather_data_2019.csv', 'weather_data_2020.csv',
#                             'weather_data_2021.csv')
#
#     dfs[1].rename(columns={'Груз': 'Марка'}, inplace=True)
#     dfs[0].rename(columns={'Наим. ЕТСНГ' : 'Марка'}, inplace=True)
#     dfs[0]['ВыгрузкаНаСклад'] = pd.to_datetime(dfs[0]['ВыгрузкаНаСклад'], errors='coerce')
#     dfs[0]['ПогрузкаНаСудно'] = pd.to_datetime(dfs[0]['ПогрузкаНаСудно'], errors='coerce')
#
#     dfs[1]['Нач.форм.штабеля'] = pd.to_datetime(dfs[1]['Нач.форм.штабеля'], errors='coerce')
#     dfs[1]['Дата начала'] = pd.to_datetime(dfs[1]['Дата начала'], errors='coerce').dt.normalize()
#     dfs[1].rename({"Дата начала": "Дата"}, inplace=True)
#
#     dfs[0]['Масса угля'] = 0
#
#     for i in dfs[0].index:
#         dfs[0]['Масса угля'][i] = calculate_coal_balance(dfs[0].loc[i], dfs[0])
#
#     storage = dfs[0].copy()
#     storage.drop_duplicates(subset='Масса угля', keep='last', inplace=True)
#     storage = storage.filter(['ВыгрузкаНаСклад', 'Марка', 'Штабель', 'Склад', 'Масса угля'])
#     storage.rename(columns={'ВыгрузкаНаСклад': 'Дата'}, inplace=True)
#
#     all_dates = pd.date_range(start='2019-01-01', end='2020-09-30', freq='D')
#
#     active_combinations = storage[['Марка', 'Штабель', 'Склад']].drop_duplicates()
#
#     active_combinations['key'] = 1
#     all_dates_df = pd.DataFrame({'Дата': all_dates})
#     all_dates_df['key'] = 1
#
#     full_grid = all_dates_df.merge(active_combinations, on='key').drop(columns='key')
#
#     df_full = full_grid.merge(storage, on=['Дата', 'Марка', 'Штабель', 'Склад'], how='left')
#
#     df_full['Масса угля'] = df_full.groupby(['Марка', 'Штабель', 'Склад'])['Масса угля'].ffill()
#
#     df_result = df_full.sort_values(['Марка', 'Штабель', 'Склад', 'Дата']).reset_index(drop=True)
#
#     full_data = df_result.merge(dfs[1], how='left',
#                                 left_on=['Дата', 'Марка', 'Штабель', 'Склад'],
#                                 right_on=['Дата начала', 'Марка', 'Штабель', 'Склад'])
#
#     full_data['Возгорание'] = (~full_data['Дата начала'].isnull()).astype(int)
#     full_data = full_data.dropna(subset=['Масса угля']).sort_values(['Дата'])
#     full_data.drop_duplicates(subset=['Масса угля', 'Дата'], inplace=True)
#     full_data = full_data[full_data['Масса угля'] != 0]
#
#     return full_data


def prepare_and_merge_data():
    import pandas as pd

    # Чтение файлов
    dfs = read_multiple_csv('supplies.csv', "fires.csv", 'temperature.csv',
                            'weather_data_2015.csv', 'weather_data_2016.csv', 'weather_data_2017.csv',
                            'weather_data_2018.csv', 'weather_data_2019.csv', 'weather_data_2020.csv',
                            'weather_data_2021.csv')

    supplies = dfs['supplies.csv']
    fires = dfs['fires.csv']
    temperature = dfs['temperature.csv']
    weather = pd.concat([dfs[f] for f in dfs if 'weather_data' in f])

    # Переименование для единообразия
    supplies.rename(columns={'Наим. ЕТСНГ': 'Марка'}, inplace=True)
    fires.rename(columns={'Груз': 'Марка'}, inplace=True)

    # Сортировка
    temperature.sort_values(['Дата акта'], inplace=True)
    supplies.sort_values(['ВыгрузкаНаСклад'], inplace=True)
    fires.sort_values(['Дата начала'], inplace=True)

    min_date = temperature['Дата акта'].iloc[0]
    max_date = temperature['Дата акта'].iloc[-1]

    # Расчёт баланса угля
    def calculate_coal_balance(row):
        current_date = row['ВыгрузкаНаСклад']
        warehouse = row['Склад']
        pile = row['Штабель']
        cargo_type = row['Марка']

        incoming = supplies[
            (supplies['ВыгрузкаНаСклад'] <= current_date)
            & (supplies['Склад'] == warehouse)
            & (supplies['Штабель'] == pile)
            & (supplies['Марка'] == cargo_type)
            ]['На склад, тн'].sum()

        outgoing = supplies[
            (supplies['ПогрузкаНаСудно'] <= current_date)
            & (supplies['Склад'] == warehouse)
            & (supplies['Штабель'] == pile)
            & (supplies['Марка'] == cargo_type)
            ]['На судно, тн'].sum()

        return incoming - outgoing

    supplies['Масса угля'] = supplies.apply(calculate_coal_balance, axis=1)

    storage = supplies.copy()
    storage.drop_duplicates(subset='Масса угля', keep='last', inplace=True)
    storage = storage.filter(['ВыгрузкаНаСклад', 'Марка', 'Штабель', 'Склад', 'Масса угля'])
    storage.rename(columns={'ВыгрузкаНаСклад': 'Дата'}, inplace=True)

    # Создание полной сетки по датам
    all_dates = pd.date_range(start=min_date, end=max_date, freq='D')
    active_combinations = storage[['Марка', 'Штабель', 'Склад']].drop_duplicates()
    active_combinations['key'] = 1
    all_dates_df = pd.DataFrame({'Дата': all_dates})
    all_dates_df['key'] = 1
    full_grid = all_dates_df.merge(active_combinations, on='key').drop(columns='key')

    df_full = full_grid.merge(storage, on=['Дата', 'Марка', 'Штабель', 'Склад'], how='left')
    df_full['Масса угля'] = df_full.groupby(['Марка', 'Штабель', 'Склад'])['Масса угля'].ffill()
    df_result = df_full.sort_values(['Марка', 'Штабель', 'Склад', 'Дата']).reset_index(drop=True)

    # Объединение с пожарами
    fires = fires[(fires['Дата начала'] >= min_date) & (fires['Дата начала'] <= max_date)]
    fires['Дата начала'] = fires['Дата начала'].dt.normalize()

    full_data = df_result.merge(fires, how='left',
                                left_on=['Дата', 'Марка', 'Штабель', 'Склад'],
                                right_on=['Дата начала', 'Марка', 'Штабель', 'Склад'])
    full_data['Возгорание'] = (~full_data['Дата начала'].isnull()).astype(int)

    return full_data
