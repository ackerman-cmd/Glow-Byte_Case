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


def prepare_and_merge_data():

    # Чтение файлов
    dfs = read_multiple_csv('../data/supplies.csv', "../data/fires.csv", '../data/temperature.csv',
                            '../data/weather_data_2015.csv', '../data/weather_data_2016.csv', '../data/weather_data_2017.csv',
                            '../data/weather_data_2018.csv', '../data/weather_data_2019.csv', '../data/weather_data_2020.csv',
                            '../data/weather_data_2021.csv')

    supplies = dfs[0]
    fires = dfs[1]
    temperature = dfs[2]
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

    # Объединение с погодой
    weather['date'] = pd.to_datetime(weather['date']).dt.normalize()
    weather_daily = weather.groupby('date').agg({
        't': 'mean',
        'humidity': 'mean',
        'precipitation': 'sum',
        'v_avg': 'mean',
        'weather_code': lambda x: x.mode().iloc[0] if not x.mode().empty else None
    }).reset_index().rename(columns={'date': 'Дата'})

    full_data = full_data.merge(weather_daily, on='Дата', how='left')

    return full_data
